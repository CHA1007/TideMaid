package com.chadate.tidemaid.client.renderer.chatbubble;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.EntityGraphics;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.IChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemChatBubbleRenderer implements IChatBubbleRenderer {
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private final ItemStack itemStack;

    public ItemChatBubbleRenderer(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public void render(EntityMaidRenderer renderer, EntityGraphics graphics) {
        if (itemStack.isEmpty()) return;

        PoseStack poseStack = graphics.getPoseStack();
        MultiBufferSource bufferSource = graphics.getBufferSource();
        // 使用最大亮度值，确保物品渲染明亮
        int packedLight = 0xF000F0;

        poseStack.pushPose();
        poseStack.translate(WIDTH / 2f, HEIGHT / 2f, 0);
        poseStack.scale(12.0f, 12.0f, 12.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(itemStack, ItemDisplayContext.GUI, packedLight, 0, poseStack, bufferSource, null, 0);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return IChatBubbleData.TYPE_2;
    }
}
