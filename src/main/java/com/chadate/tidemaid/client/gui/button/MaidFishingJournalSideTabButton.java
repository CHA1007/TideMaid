package com.chadate.tidemaid.client.gui.button;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.gui.ITooltipButton;
import com.li64.tide.registries.TideItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 女仆百鱼全书侧边栏按钮
 */
public class MaidFishingJournalSideTabButton extends Button implements ITooltipButton {
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_side.png");
    private static final int V_OFFSET = 107;
    private static final int INDEX = 2;
    private static final int UNACTIVE_BG_X = 234;
    private final int top;
    private final List<Component> tooltips;
    private final ItemStack journalIcon;

    public MaidFishingJournalSideTabButton(int x, int y, OnPress onPress) {
        super(Button.builder(Component.empty(), onPress).pos(x, y).size(26, 24));
        this.top = V_OFFSET + INDEX * 25;
        this.tooltips = List.of(
                Component.translatable("gui.tidemaid.button.fishing_journal"),
                Component.translatable("gui.tidemaid.button.fishing_journal.desc")
        );
        this.journalIcon = new ItemStack(TideItems.FISHING_JOURNAL);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableDepthTest();
        graphics.blit(SIDE, this.getX() + 6, this.getY(), UNACTIVE_BG_X, top, this.width, this.height, 256, 256);
        graphics.pose().pushPose();
        graphics.pose().scale(0.9f, 0.9f, 1f);
        graphics.renderItem(journalIcon, (int) ((this.getX() + 9) / 0.9f), (int) ((this.getY() + 4.5) / 0.9f));
        graphics.pose().popPose();
    }

    @Override
    public boolean isTooltipHovered() {
        return this.isHovered();
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, Minecraft mc, int mouseX, int mouseY) {
        graphics.renderComponentTooltip(mc.font, this.tooltips, mouseX, mouseY);
    }
}
