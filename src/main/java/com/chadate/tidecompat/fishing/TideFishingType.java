package com.chadate.tidecompat.fishing;

import com.chadate.tidecompat.task.TaskTideFishing;
import com.chadate.tidecompat.util.TideFishingUtils;
import com.chadate.tidecompat.entity.TideMaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.api.entity.fishing.IFishingType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.registries.items.TideFishingRodItem;
import com.li64.tide.registries.TideItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tide钓鱼类型实现
 * 用于将Tide MOD的钓鱼机制集成到Touhou Little Maid中
 */
public class TideFishingType implements IFishingType {

    @Override
    public boolean isFishingRod(ItemStack itemStack) {
        return itemStack.getItem() instanceof TideFishingRodItem ||
               isTideMidasRod(itemStack);
    }

    @Override
    public boolean suitableFishingHook(EntityMaid maid, Level level, ItemStack rod, net.minecraft.core.BlockPos pos) {
        var fluidState = level.getFluidState(pos);

        if (fluidState.is(net.minecraft.tags.FluidTags.WATER)) {
            return true;
        }

        boolean isTideTask = maid.getTask().getUid().equals(TaskTideFishing.UID);
        if (!isTideTask) {
            return false;
        }

        if (fluidState.is(net.minecraft.tags.FluidTags.LAVA)) {
            return TideFishingUtils.canFishInLava(rod);
        }

        return TideFishingUtils.isPositionInVoid(level, pos) && TideFishingUtils.canFishInVoid(rod);
    }

    @Override
    public MaidFishingHook getFishingHook(EntityMaid maid, Level level, ItemStack itemStack, Vec3 pos) {
        int luckBonus = EnchantmentHelper.getFishingLuckBonus((ServerLevel) level, itemStack, maid);
        int lureSpeedBonus = (int) (EnchantmentHelper.getFishingTimeReduction((ServerLevel) level, itemStack, maid) * 20.0F);

        Vec3 adjustedPos = TideFishingUtils.getBobberSpawnPosition(level, pos);
        return new TideMaidFishingHook(maid, level, luckBonus, lureSpeedBonus, adjustedPos);
    }

    private boolean isTideMidasRod(ItemStack stack) {
        return TideItems.MIDAS_FISHING_ROD == stack.getItem();
    }
}