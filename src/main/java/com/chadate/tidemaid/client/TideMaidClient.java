package com.chadate.tidemaid.client;

import com.chadate.tidemaid.client.gui.screens.MaidFishingJournal;
import com.chadate.tidemaid.data.MaidFishingData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.client.TideItemModelProperties;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;

/**
 * 客户端初始化 - 注册鱼竿模型属性和数据同步回调
 */
public class TideMaidClient {

    /**
     * 客户端初始化入口
     */
    public static void init() {
        registerModelProperties();
        registerDataSyncCallback();
    }

    /**
     * 注册数据同步回调，用于在网络包收到数据后刷新 UI
     */
    private static void registerDataSyncCallback() {
        MaidFishingData.setDataSyncCallback(maidUuid -> {
            if (Minecraft.getInstance().screen instanceof MaidFishingJournal journal) {
                journal.onDataSync(maidUuid);
            }
        });
    }

    /**
     * 注册 tide 鱼竿的 cast 模型属性
     */
    public static void registerModelProperties() {
        ClampedItemPropertyFunction castFunc = (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            boolean mainHand = entity.getMainHandItem() == stack;
            boolean offHand = entity.getOffhandItem() == stack;

            if (entity.getMainHandItem().getItem() instanceof FishingRodItem ||
                entity.getMainHandItem().getItem() instanceof TideFishingRodItem) {
                offHand = false;
            }

            if (entity instanceof Player p) {
                return (mainHand || offHand) && p.fishing != null ? 1.0F : 0.0F;
            } else if (entity instanceof EntityMaid maid) {
                return (mainHand || offHand) && maid.fishing != null ? 1.0F : 0.0F;
            }
            return 0.0F;
        };

        Item[] rods = {
                TideItems.STONE_FISHING_ROD,
                TideItems.IRON_FISHING_ROD,
                TideItems.GOLDEN_FISHING_ROD,
                TideItems.CRYSTAL_FISHING_ROD,
                TideItems.DIAMOND_FISHING_ROD,
                TideItems.NETHERITE_FISHING_ROD,
                TideItems.MIDAS_FISHING_ROD
        };

        for (Item rod : rods) {
            registerRod(rod, castFunc);
        }
    }

    private static void registerRod(Item item, ClampedItemPropertyFunction func) {
        ItemProperties.register(item, TideItemModelProperties.CAST_PROPERTY, func);
    }
}
