package com.chadate.tidemaid.util;

import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tide 钓鱼相关工具方法
 */
public final class TideFishingUtil {

    private TideFishingUtil() {}

    public static final int LURE_SPEED_TO_TICKS = 20;

    /**
     * 检查鱼竿是否防岩浆
     */
    public static boolean isLavaproof(ItemStack rod) {
        if (rod.getItem() instanceof TideFishingRodItem r) return r.isLavaproof(rod);
        return false;
    }

    /**
     * 检查鱼竿是否防虚空
     */
    public static boolean isVoidproof(ItemStack rod) {
        if (rod.getItem() instanceof TideFishingRodItem r) return r.isVoidproof(rod);
        return false;
    }

    /**
     * 获取虚空表面高度
     */
    public static int getVoidSurface(Level level) {
        return FishingMedium.VOID.getVoidSurface(level);
    }

    /**
     * 判断实体是否在虚空中
     */
    public static boolean isInVoid(Entity entity, Level level) {
        return entity.getY() <= getVoidSurface(level);
    }

    /**
     * 判断位置是否适合虚空钓鱼
     */
    public static boolean isVoidFishingSpot(BlockPos pos, Level level) {
        int voidSurface = getVoidSurface(level);
        if (voidSurface >= level.getMinBuildHeight()) {
            return pos.getY() <= voidSurface && pos.getY() >= level.getMinBuildHeight();
        } else {
            return pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMinBuildHeight() + 5;
        }
    }
}
