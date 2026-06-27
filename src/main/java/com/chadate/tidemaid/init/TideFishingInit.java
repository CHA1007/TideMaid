package com.chadate.tidemaid.init;

import com.chadate.tidemaid.fishing.TideFishingType;
import com.chadate.tidemaid.fishing.rod.RodBehaviorRegistry;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.fishing.FishingTypeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 初始化 tide 钓鱼类型和实体注册
 */
public class TideFishingInit {

    public static void init(IEventBus modEventBus) {
        TideEntities.register(modEventBus);
        modEventBus.addListener(TideFishingInit::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            RodBehaviorRegistry.init();
            new FishingTypeManager().addFishingType(new TideFishingType());
        });
    }
}
