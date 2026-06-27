package com.chadate.tidemaid.events;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.data.MaidFishingData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TideMaid.MOD_ID, value = Dist.DEDICATED_SERVER)
public class PlayerLogoutHandler {

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MaidFishingData.clearPlayerCache(event.getEntity().getUUID());
    }
}
