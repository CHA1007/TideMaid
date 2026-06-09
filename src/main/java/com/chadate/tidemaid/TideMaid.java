package com.chadate.tidemaid;

import com.chadate.tidemaid.client.TideMaidClient;
import com.chadate.tidemaid.client.model.TideFishingModel;
import com.chadate.tidemaid.client.renderer.TideMaidFishingHookRenderer;
import com.chadate.tidemaid.init.TideEntities;
import com.chadate.tidemaid.init.TideFishingInit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(TideMaid.MOD_ID)
public class TideMaid {
    public static final String MOD_ID = "tidemaid";

    public TideMaid(IEventBus modEventBus) {
        TideFishingInit.init(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
            modEventBus.addListener(this::registerEntityRenderers);
            modEventBus.addListener(this::registerLayerDefinitions);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(TideMaidClient::registerModelProperties);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TideEntities.TIDE_MAID_FISHING_HOOK.get(), TideMaidFishingHookRenderer::new);
    }

    private void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TideFishingModel.MODEL_LOCATION, TideFishingModel::createBodyLayer);
    }
}
