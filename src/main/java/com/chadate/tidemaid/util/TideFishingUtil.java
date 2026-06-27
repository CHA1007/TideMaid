package com.chadate.tidemaid.util;

import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.data.rods.BaitContents;
import com.li64.tide.registries.items.TideFishingRodItem;
import com.li64.tide.util.BaitUtils;
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

    /**
     * 检查鱼竿上是否有提供运气加成的鱼饵
     */
    public static boolean hasLuckBait(ItemStack rod) {
        BaitContents contents = TideItemData.BAIT_CONTENTS.get(rod);
        if (contents == null) return false;
        
        for (ItemStack bait : contents.items()) {
            if (!bait.isEmpty() && BaitUtils.getBaitLuck(bait) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查鱼竿上是否有提供宝箱概率的鱼饵
     */
    public static boolean hasCrateBait(ItemStack rod) {
        BaitContents contents = TideItemData.BAIT_CONTENTS.get(rod);
        if (contents == null) return false;
        
        for (ItemStack bait : contents.items()) {
            if (!bait.isEmpty() && BaitUtils.getCrateChance(bait) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取鱼竿上所有鱼饵的总宝箱概率
     */
    public static int getCombinedCrateChance(ItemStack rod) {
        BaitContents contents = TideItemData.BAIT_CONTENTS.get(rod);
        if (contents == null) return 0;
        
        int totalChance = 0;
        for (ItemStack bait : contents.items()) {
            if (!bait.isEmpty()) {
                totalChance += BaitUtils.getCrateChance(bait);
            }
        }
        return totalChance;
    }
}
