package com.chadate.tidemaid.fishing;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.chadate.tidemaid.fishing.rod.RodBehavior;
import com.chadate.tidemaid.fishing.rod.RodBehaviorRegistry;
import com.chadate.tidemaid.fishing.rod.RodStats;
import com.chadate.tidemaid.task.TaskTideFishing;
import com.chadate.tidemaid.util.TideFishingUtil;
import com.github.tartaricacid.touhoulittlemaid.api.entity.fishing.IFishingType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.data.rods.BaitContents;
import com.li64.tide.data.rods.CustomRodManager;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.items.TideFishingRodItem;
import com.li64.tide.util.BaitUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tide 钓鱼类型
 */
public class TideFishingType implements IFishingType {

    @Override
    public boolean isFishingRod(ItemStack stack) {
        return stack.getItem() instanceof TideFishingRodItem || stack.is(TideItems.MIDAS_FISHING_ROD);
    }

    @Override
    public boolean suitableFishingHook(EntityMaid maid, Level level, ItemStack rod, BlockPos pos) {
        boolean isTideTask = maid.getTask().getUid().equals(TaskTideFishing.UID);

        var fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.WATER)) return true;

        if (!isTideTask) return false;

        if (fluid.is(FluidTags.LAVA)) return TideFishingUtil.isLavaproof(rod);

        if (TideFishingUtil.isVoidproof(rod) && TideFishingUtil.isVoidFishingSpot(pos, level)) {
            var blockState = level.getBlockState(pos);
            return blockState.isAir() || blockState.getCollisionShape(level, pos).isEmpty();
        }
        return false;
    }

    @Override
    public MaidFishingHook getFishingHook(EntityMaid maid, Level level, ItemStack rod, Vec3 pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new TideMaidFishingHook(maid, level, 0, 0, pos);
        }

        int luck = EnchantmentHelper.getFishingLuckBonus(serverLevel, rod, maid);
        int speed = (int) (EnchantmentHelper.getFishingTimeReduction(serverLevel, rod, maid) / 5f);

        // 使用策略模式应用鱼竿特殊属性加成
        RodBehavior behavior = RodBehaviorRegistry.getBehavior(rod);
        RodStats rodStats = new RodStats();
        behavior.modifyStats(rodStats, level, maid, rod);
        luck += rodStats.getLuckBonus();
        speed += rodStats.getSpeedBonus();

        ItemStack line = CustomRodManager.getLine(rod);
        if (line.is(TideItems.GOLDEN_LINE)) {
            luck += 1;
        }

        BaitContents baitContents = TideItemData.BAIT_CONTENTS.get(rod);
        if (baitContents != null) {
            for (ItemStack bait : baitContents.items()) {
                if (!bait.isEmpty()) {
                    speed += BaitUtils.getBaitSpeed(bait);
                    luck += BaitUtils.getBaitLuck(bait);
                }
            }
        }

        Vec3 finalPos = pos;
        if (TideFishingUtil.isVoidproof(rod) && TideFishingUtil.isVoidFishingSpot(BlockPos.containing(pos), level)) {
            int voidSurface = TideFishingUtil.getVoidSurface(level);
            finalPos = new Vec3(pos.x, voidSurface, pos.z);
        }

        return new TideMaidFishingHook(maid, level, luck, speed * TideFishingUtil.LURE_SPEED_TO_TICKS, finalPos);
    }
}
