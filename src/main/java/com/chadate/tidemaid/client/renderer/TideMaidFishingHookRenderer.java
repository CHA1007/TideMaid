package com.chadate.tidemaid.client.renderer;

import com.chadate.tidemaid.client.model.TideFishingModel;
import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.Tide;
import com.li64.tide.data.rods.CustomRodManager;
import com.li64.tide.registries.items.FishingBobberItem;
import com.li64.tide.registries.items.FishingHookItem;
import com.li64.tide.registries.items.FishingLineItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Tide 女仆钓鱼钩渲染器
 */
public class TideMaidFishingHookRenderer extends EntityRenderer<TideMaidFishingHook> {
    private static final ResourceLocation DEFAULT_HOOK_TEX = Tide.resource("textures/entity/fishing_hook/fishing_hook.png");
    private final ItemRenderer itemRenderer;
    private final TideFishingModel<TideMaidFishingHook> tideModel;

    public TideMaidFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.tideModel = new TideFishingModel<>(context.bakeLayer(TideFishingModel.MODEL_LOCATION));
        this.shadowRadius = 0.1f;
    }

    /**
     * 获取鱼钩纹理：从 Tide 鱼竿的鱼钩配件读取
     */
    private ResourceLocation getHookTexture(TideMaidFishingHook hook) {
        EntityMaid maid = hook.getMaidOwner();
        if (maid != null) {
            ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack hookItem = CustomRodManager.getHook(rod);
            if (!hookItem.isEmpty()) {
                return FishingHookItem.getTexture(hookItem);
            }
        }
        return DEFAULT_HOOK_TEX;
    }

    /**
     * 获取鱼线颜色
     */
    private String getLineColorHex(TideMaidFishingHook hook) {
        EntityMaid maid = hook.getMaidOwner();
        if (maid != null) {
            ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack lineItem = CustomRodManager.getLine(rod);
            if (!lineItem.isEmpty()) {
                return FishingLineItem.getColor(lineItem);
            }
        }
        return "#D6D6D6";
    }

    @Override
    public void render(TideMaidFishingHook hook, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        EntityMaid maid = hook.getMaidOwner();
        if (maid == null) return;

        ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack bobber = CustomRodManager.getBobber(rod);

        poseStack.pushPose();
        poseStack.pushPose();

        poseStack.translate((1f / 16f) / 2f, 0, (1f / 16f) / 2f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - maid.yBodyRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        tideModel.setupAnim(hook, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);

        ResourceLocation hookTex = getHookTexture(hook);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(hookTex));

        tideModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255, 255, 255, 255));

        if (!bobber.isEmpty() && FishingBobberItem.renderItemModel(bobber)) {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.translate(0.03f, -0.22f, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            this.itemRenderer.renderStatic(bobber,
                    ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, hook.level(), hook.getId());
            poseStack.popPose();
        }

        poseStack.popPose();

        renderConnectingString(hook, partialTick, poseStack, buffer, maid);

        poseStack.popPose();

        super.render(hook, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * 渲染鱼线
     */
    private void renderConnectingString(TideMaidFishingHook hook, float partialTick, PoseStack poseStack, MultiBufferSource buffer, EntityMaid maid) {
        float lerpBodyRot;
        lerpBodyRot = Mth.lerp(partialTick, maid.yBodyRotO, maid.yBodyRot) * (float) (Math.PI / 180.0);
        double sin = Mth.sin(lerpBodyRot);
        double cos = Mth.cos(lerpBodyRot);

        double x1 = Mth.lerp(partialTick, maid.xo, maid.getX()) - cos * 0.35D - sin * 0.8D;
        double y1 = Mth.lerp(partialTick, maid.yo, maid.getY()) + maid.getEyeHeight() - 0.45D;
        double z1 = Mth.lerp(partialTick, maid.zo, maid.getZ()) - sin * 0.35D + cos * 0.8D;

        double x2 = Mth.lerp(partialTick, hook.xo, hook.getX());
        double y2 = Mth.lerp(partialTick, hook.yo, hook.getY()) + 0.25D;
        double z2 = Mth.lerp(partialTick, hook.zo, hook.getZ());

        float dx = (float) (x1 - x2);
        float dy = (float) (y1 - y2) - 0.1875F;
        float dz = (float) (z1 - z2);
        
        String lineColorHex = getLineColorHex(hook);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose pose = poseStack.last();

        for (int k = 0; k <= 16; k++) {
            stringVertex(dx, dy, dz, vertexConsumer, pose, fraction(k), fraction(k + 1), lineColorHex, maid.level(), maid.blockPosition().above(), partialTick);
        }

        // Iris/Oculus 着色器修复：断开鱼线连接，防止不同鱼钩的鱼线互相连接
        if (Tide.PLATFORM.isModLoaded("iris") || Tide.PLATFORM.isModLoaded("oculus")) {
            vertexConsumer.addVertex(0, 0, 0)
                    .setColor(0, 0, 0, 255)
                    .setNormal(0, 0, 0);
        }
    }

    private static float fraction(int pNumerator) {
        return (float)pNumerator / (float) 16;
    }

    private static void stringVertex(float x, float y, float z, VertexConsumer vertexConsumer, PoseStack.Pose pose, float frac1, float frac2, String colorHex, net.minecraft.world.level.Level level, BlockPos samplePos, float partialTick) {
        float f = x * frac1;
        float f1 = y * (frac1 * frac1 + frac1) * 0.5F + 0.25F;
        float f2 = z * frac1;
        float f3 = x * frac2 - f;
        float f4 = y * (frac2 * frac2 + frac2) * 0.5F + 0.25F - f1;
        float f5 = z * frac2 - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);

        f3 /= f6;
        f4 /= f6;
        f5 /= f6;

        Color color = Color.decode(colorHex);

        float skyDarken = level instanceof ClientLevel cl ? (1 - cl.getSkyDarken(partialTick)) * 15 : 0;
        float blockBrightness = level.getBrightness(LightLayer.BLOCK, samplePos);
        float skyBrightness = level.getBrightness(LightLayer.SKY, samplePos) - skyDarken + 1;

        float colorBrightness = Tide.CLIENT_CONFIG.general.defaultLineColor ? 0.0f : Mth.clamp(
                Math.max(blockBrightness, skyBrightness) / 15f,
                level.dimensionType().ambientLight(), 1f);

        int r = (int) (color.getRed() * colorBrightness);
        int g = (int) (color.getGreen() * colorBrightness);
        int b = (int) (color.getBlue() * colorBrightness);

        vertexConsumer.addVertex(pose.pose(), f, f1, f2)
                .setColor(FastColor.ARGB32.color(255, r, g, b))
                .setNormal(pose, f3, f4, f5);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TideMaidFishingHook hook) {
        return DEFAULT_HOOK_TEX;
    }
}
