package com.chadate.tidecompat.init;

import com.chadate.tidecompat.fishing.TideFishingType;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.fishing.FishingTypeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class TideFishingInit {

    public static void init(IEventBus modEventBus) {
        TideEntities.register(modEventBus);
        
        modEventBus.addListener(TideFishingInit::setup);
    }

    private static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(TideFishingInit::registerFishingTypes);
    }

    private static void registerFishingTypes() {
        FishingTypeManager manager = new FishingTypeManager();
        manager.addFishingType(new TideFishingType());
    }
}