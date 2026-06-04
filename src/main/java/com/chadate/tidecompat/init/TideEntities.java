package com.chadate.tidecompat.init;

import com.chadate.tidecompat.TideTouhoulittlemaidCompat;
import com.chadate.tidecompat.entity.TideMaidFishingHook;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * TideCompat 实体注册
 */
public class TideEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TideTouhoulittlemaidCompat.MOD_ID);

    public static final Supplier<EntityType<TideMaidFishingHook>> TIDE_MAID_FISHING_HOOK =
            ENTITY_TYPES.register("tide_maid_fishing_hook", () ->
                    EntityType.Builder.<TideMaidFishingHook>of(TideMaidFishingHook::new, MobCategory.MISC)
                            .noSave().noSummon().sized(0.25F, 0.25F)
                            .clientTrackingRange(4).updateInterval(5)
                            .build("tide_maid_fishing_hook")
            );

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}