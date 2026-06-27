package com.chadate.tidemaid.fishing.rod;

import com.li64.tide.registries.TideItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 鱼竿行为注册表
 * 管理所有鱼竿的行为策略，提供统一的查询接口。
 */
public class RodBehaviorRegistry {

    private static final Map<Item, RodBehavior> BEHAVIORS = new HashMap<>();

    /**
     * 初始化注册表，注册所有鱼竿行为
     */
    public static void init() {
        // 属性修正类
        register(TideItems.GOLDEN_FISHING_ROD, new GoldenRodBehavior());
        register(TideItems.MIDAS_FISHING_ROD, new GoldenRodBehavior());
        register(TideItems.PRISMARINE_FISHING_ROD, new PrismarineRodBehavior());
        register(TideItems.SUNFLOWER_FISHING_ROD, new SunflowerRodBehavior());

        // 音效类
        register(TideItems.CRYSTAL_FISHING_ROD, new CrystalRodBehavior());
        register(TideItems.ECHO_FISHING_ROD, new EchoRodBehavior());

        // 粒子类
        register(TideItems.BLAZING_FISHING_ROD, new BlazingRodBehavior());
        register(TideItems.HONEYCOMB_FISHING_ROD, new HoneycombRodBehavior());

        // 额外奖励类
        register(TideItems.DIAMOND_FISHING_ROD, new DiamondRodBehavior());

        // MIDAS 同时具有属性修正和额外奖励，需要组合行为
        register(TideItems.MIDAS_FISHING_ROD, new CompositeRodBehavior(
                new GoldenRodBehavior(), new MidasRodBehavior()));
        register(TideItems.VILLAGE_FISHING_ROD, new VillageRodBehavior());
    }

    /**
     * 注册鱼竿行为
     */
    public static void register(Item rod, RodBehavior behavior) {
        BEHAVIORS.put(rod, behavior);
    }

    /**
     * 获取鱼竿行为
     *
     * @param rod 鱼竿物品
     * @return 鱼竿行为，如果没有注册则返回 EMPTY
     */
    public static RodBehavior getBehavior(ItemStack rod) {
        if (rod.isEmpty()) {
            return RodBehavior.EMPTY;
        }
        return BEHAVIORS.getOrDefault(rod.getItem(), RodBehavior.EMPTY);
    }

    /**
     * 获取鱼竿行为
     *
     * @param rod 鱼竿物品
     * @return 鱼竿行为，如果没有注册则返回 EMPTY
     */
    public static RodBehavior getBehavior(Item rod) {
        return BEHAVIORS.getOrDefault(rod, RodBehavior.EMPTY);
    }

    /**
     * 检查鱼竿是否已注册行为
     */
    public static boolean hasBehavior(Item rod) {
        return BEHAVIORS.containsKey(rod);
    }
}
