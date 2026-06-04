package com.chadate.tidecompat;

import com.chadate.tidecompat.client.TideCompatClient;
import com.chadate.tidecompat.client.render.DebugFishingRenderer;
import com.chadate.tidecompat.debug.DebugFishingCommand;
import com.chadate.tidecompat.init.TideEntities;
import com.chadate.tidecompat.init.TideFishingInit;
import com.chadate.tidecompat.network.DebugFishingSyncPayload;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.MaidFishingHookRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(TideTouhoulittlemaidCompat.MOD_ID)
public class TideTouhoulittlemaidCompat {
    public static final String MOD_ID = "tidecompat";

    public TideTouhoulittlemaidCompat(IEventBus modEventBus) {
        TideFishingInit.init(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        modEventBus.addListener(this::registerPayloadHandlers);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
            modEventBus.addListener(this::registerEntityRenderers);
            NeoForge.EVENT_BUS.register(DebugFishingRenderer.class);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(TideCompatClient::registerModelProperties);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TideEntities.TIDE_MAID_FISHING_HOOK.get(), MaidFishingHookRenderer::new);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        DebugFishingCommand.register(event.getDispatcher());
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToClient(DebugFishingSyncPayload.TYPE, DebugFishingSyncPayload.STREAM_CODEC,
                DebugFishingSyncPayload::handle);
    }
}
