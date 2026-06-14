package com.chadate.tidemaid.events;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.client.gui.button.MaidFishingJournalSideTabButton;
import com.chadate.tidemaid.client.gui.screens.MaidFishingJournal;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.MaidContainerGuiEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 女仆GUI兼容性事件监听器
 */
@EventBusSubscriber(modid = TideMaid.MOD_ID, value = Dist.CLIENT)
public class MaidGuiCompatEvents {
    
    @SubscribeEvent
    public static void onMaidGuiInit(MaidContainerGuiEvent.Init event) {
        AbstractMaidContainerGui<?> screen = event.getGui();
        int leftPos = event.getLeftPos();
        int topPos = event.getTopPos();
        int buttonX = leftPos + 249;
        int buttonY = topPos + 28 + 9 + 50;
        
        MaidFishingJournalSideTabButton journalButton = new MaidFishingJournalSideTabButton(
                buttonX,
                buttonY,
                button -> openFishingJournal(screen)
        );
        
        event.addButton("fishing_journal", journalButton);
    }
    
    private static void openFishingJournal(AbstractMaidContainerGui<?> screen) {
        EntityMaid maid = screen.getMaid();
        Player player = Minecraft.getInstance().player;
        if (player != null && maid != null) {
            Minecraft.getInstance().setScreen(new MaidFishingJournal(player, maid));
        }
    }
}
