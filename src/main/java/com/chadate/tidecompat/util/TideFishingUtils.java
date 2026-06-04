package com.chadate.tidecompat.util;

import com.li64.tide.Tide;
import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TideFishingUtils {

    public static boolean canFishInLava(ItemStack rod) {
        if (rod.getItem() instanceof TideFishingRodItem tideRodItem) {
            return tideRodItem.isLavaproof(rod);
        }
        return false;
    }

    public static boolean canFishInVoid(ItemStack rod) {
        if (rod.getItem() instanceof TideFishingRodItem tideRodItem) {
            return tideRodItem.isVoidproof(rod);
        }
        return false;
    }

    public static int getVoidSurface(Level level) {
        var entries = Tide.CONFIG.general.fishableVoidHeights;
        String dimId = level.dimension().location().toString();
        for (var entry : entries) {
            if (entry.dimension.equals(dimId)) {
                return switch (entry.type) {
                    case RELATIVE_TO_BOTTOM -> level.getMinBuildHeight() + entry.height;
                    case RELATIVE_TO_TOP -> level.getMaxBuildHeight() + entry.height;
                    case ABSOLUTE -> entry.height;
                };
            }
        }
        return level.getMinBuildHeight() - 6;
    }

    public static boolean isPositionInVoid(Level level, BlockPos pos) {
        return pos.getY() <= getVoidSurface(level);
    }

    public static Vec3 getBobberSpawnPosition(Level level, Vec3 targetPos) {
        BlockPos blockPos = BlockPos.containing(targetPos);
        var fluidState = level.getFluidState(blockPos);

        if (fluidState.is(FluidTags.WATER) || fluidState.is(FluidTags.LAVA)) {
            double surfaceY = blockPos.getY() + fluidState.getHeight(level, blockPos);
            return new Vec3(targetPos.x, surfaceY + 0.05, targetPos.z);
        }

        if (isPositionInVoid(level, blockPos)) {
            int voidSurface = getVoidSurface(level);
            return new Vec3(targetPos.x, voidSurface - 0.1, targetPos.z);
        }

        return new Vec3(targetPos.x, blockPos.getY() + 1.0, targetPos.z);
    }

    public static Vec3 getBobberSpawnPosition(Level level, BlockPos blockPos) {
        return getBobberSpawnPosition(level, Vec3.atCenterOf(blockPos));
    }
}
