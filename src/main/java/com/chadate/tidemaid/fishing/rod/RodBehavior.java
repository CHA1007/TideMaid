package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 鱼竿行为策略接口
 */
public interface RodBehavior {

    /**
     * 修改鱼竿的钓鱼属性（luck/speed 加成）
     *
     * @param stats 属性数据，可修改 luckBonus 和 speedBonus
     * @param level 世界
     * @param maid  女仆
     * @param rod   鱼竿物品
     */
    default void modifyStats(RodStats stats, Level level, EntityMaid maid, ItemStack rod) {}

    /**
     * 咬钩时触发的效果
     *
     * @param hook  鱼钩实体
     * @param level 服务端世界
     */
    default void onNibble(TideMaidFishingHook hook, ServerLevel level) {}

    /**
     * 等待咬钩时的粒子效果自定义
     *
     * @param hook   鱼钩实体
     * @param level  服务端世界
     * @param medium 当前钓鱼介质
     * @return 是否已处理粒子效果
     */
    default boolean onWaitParticle(TideMaidFishingHook hook, ServerLevel level, FishingMedium medium) {
        return false;
    }

    /**
     * 获取钓鱼时的维度覆盖
     *
     * @param hook   鱼钩实体
     * @param level  服务端世界
     * @param medium 当前钓鱼介质
     * @return 维度 key，null 表示不覆盖
     */
    @Nullable
    default ResourceKey<Level> getFishingDimension(TideMaidFishingHook hook, ServerLevel level, FishingMedium medium) {
        return null;
    }

    /**
     * 添加额外奖励物品
     *
     * @param hook      鱼钩实体
     * @param level     服务端世界
     * @param loot      当前渔获列表
     * @param lootParams 战利品参数
     * @param result    渔获结果
     */
    default void addBonusLoot(TideMaidFishingHook hook, ServerLevel level, List<ItemStack> loot, 
                              LootParams lootParams, CatchResult result) {}

    /**
     * 钓到鱼时的处理
     *
     * @param hook   鱼钩实体
     * @param result 渔获结果
     */
    default void onCatch(TideMaidFishingHook hook, CatchResult result) {}

    /**
     * 收竿时触发的效果
     *
     * @param hook 鱼钩实体
     */
    default void onRetrieve(TideMaidFishingHook hook) {}

    /**
     * 空行为实现，用于不需要特殊处理的鱼竿
     */
    RodBehavior EMPTY = new RodBehavior() {};
}
