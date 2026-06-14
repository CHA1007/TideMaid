package com.chadate.tidemaid.events;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.data.MaidFishingData;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.registries.items.FishSatchelItem;
import com.li64.tide.util.TideUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 监听女仆拾取物品事件，自动将渔获存入钓鱼背包
 */
@EventBusSubscriber(modid = TideMaid.MOD_ID)
public class MaidSatchelPickupHandler {

    /**
     * 在女仆拾取物品前，检测是否为渔获并尝试优先存入钓鱼背包
     */
    @SubscribeEvent
    public static void onMaidPickupItemPre(MaidPickupEvent.ItemResultPre event) {
        if (event.isSimulate()) return;

        var maid = event.getMaid();
        ItemEntity entityItem = event.getEntityItem();

        if (maid.level().isClientSide) return;

        ItemStack fishStack = entityItem.getItem();
        if (fishStack.isEmpty() || !FishSatchelItem.canPutInSatchel(fishStack)) return;

        int storedCount = tryStoreInSatchel(maid, fishStack);
        if (storedCount > 0) {
            fishStack.shrink(storedCount);

            if (fishStack.isEmpty()) {
                entityItem.discard();
            } else {
                entityItem.setItem(fishStack);
            }

            event.setCanPickup(true);
            event.setCanceled(true);
        }
    }

    /**
     * 尝试将物品存入女仆背包中的钓鱼背包
     * @return 成功存入的物品数量
     */
    private static int tryStoreInSatchel(EntityMaid maid, ItemStack stack) {
        if (!stack.getItem().canFitInsideContainerItems()) return 0;

        int storedCount = 0;
        int maxToStore = stack.getCount();
        List<ItemStack> satchelSlots = getSatchelSlots(maid);

        for (ItemStack satchel : satchelSlots) {
            if (storedCount >= maxToStore) break;
            
            // 只存入已打开的钓鱼背包
            if (!TideItemData.FISH_SATCHEL_OPENED.getOrDefault(satchel, false)) continue;

            BundleContents contents = satchel.get(DataComponents.BUNDLE_CONTENTS);
            List<ItemStack> items = contents != null ? new ArrayList<>((Collection) contents.items()) : new ArrayList<>();
            
            int currentSize = items.size();
            if (currentSize >= 64) continue;

            int canStore = Math.min(maxToStore - storedCount, 64 - currentSize);
            
            for (int j = 0; j < canStore; j++) {
                ItemStack toStore = stack.copyWithCount(1);
                boolean inserted = false;

                for (ItemStack existing : items) {
                    if (ItemStack.isSameItemSameComponents(toStore, existing) 
                            && existing.getCount() < existing.getMaxStackSize()) {
                        int canAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), 1);
                        if (canAdd > 0) {
                            existing.grow(canAdd);
                            inserted = true;
                            break;
                        }
                    }
                }
                
                if (!inserted && items.size() < 64) {
                    items.add(toStore);
                    inserted = true;
                }
                
                if (inserted) {
                    storedCount++;
                } else {
                    break;
                }
            }
            
            if (storedCount > 0) {
                satchel.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
            }
        }

        if (storedCount > 0 && maid.getOwner() instanceof ServerPlayer serverPlayer) {
            ItemStack storedStack = stack.copyWithCount(storedCount);

            if (TideUtils.isJournalFish(storedStack)) {
                MaidFishingData data = MaidFishingData.getOrCreate(serverPlayer, maid.getUUID());
                if (maid.level() instanceof ServerLevel serverLevel) {
                    data.logCatch(storedStack, serverLevel);
                }
                data.syncTo(serverPlayer);
            }
        }

        return storedCount;
    }

    /**
     * 获取女仆身上所有的钓鱼背包槽位（副手 + 背包）
     */
    private static @NotNull List<ItemStack> getSatchelSlots(EntityMaid maid) {
        List<ItemStack> satchelSlots = new ArrayList<>();

        ItemStack offhand = maid.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof FishSatchelItem) {
            satchelSlots.add(offhand);
        }

        var maidInv = maid.getMaidInv();
        for (int i = 0; i < maidInv.getSlots(); i++) {
            ItemStack slotStack = maidInv.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof FishSatchelItem) {
                satchelSlots.add(slotStack);
            }
        }
        
        return satchelSlots;
    }
}
