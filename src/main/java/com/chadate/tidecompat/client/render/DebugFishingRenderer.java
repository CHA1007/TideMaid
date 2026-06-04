package com.chadate.tidecompat.client.render;

import com.chadate.tidecompat.TideTouhoulittlemaidCompat;
import com.chadate.tidecompat.task.TaskTideFishing;
import com.chadate.tidecompat.util.TideFishingUtils;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * DEBUG 渲染器：绘制女仆潮汐钓鱼的搜索范围
 */
public class DebugFishingRenderer {

    /** 当前客户端是否启用了 DEBUG 渲染 */
    private static volatile boolean ENABLED = false;

    /** 颜色定义 */
    private static final float[] SEARCH_BOX_COLOR = {1.0F, 1.0F, 0.0F};
    private static final float[] MATCHED_BLOCK_COLOR = {0.0F, 1.0F, 0.0F};
    private static final float[] VOID_BOX_COLOR = {0.6F, 0.0F, 1.0F};

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        if (!isEnabled()) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        double renderDist = Minecraft.getInstance().options.renderDistance().get() * 16;
        AABB searchBox = new AABB(
                cameraPos.x - renderDist, cameraPos.y - renderDist, cameraPos.z - renderDist,
                cameraPos.x + renderDist, cameraPos.y + renderDist, cameraPos.z + renderDist);

        for (EntityMaid maid : level.getEntitiesOfClass(EntityMaid.class, searchBox)) {
            if (!maid.isAlive()) continue;

            if (!maid.getTask().getUid().equals(TaskTideFishing.UID)) continue;

            renderMaidFishingSearch(poseStack, bufferSource, cameraPos, maid, level);
        }
    }

    private static void renderMaidFishingSearch(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                                 Vec3 cameraPos, EntityMaid maid, Level level) {
        BlockPos maidPos = maid.blockPosition();
        ItemStack mainHandItem = maid.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        drawSearchBox(poseStack, bufferSource, maidPos, SEARCH_BOX_COLOR);

        BlockPos belowPos = maidPos.below();
        drawSearchBox(poseStack, bufferSource, belowPos, SEARCH_BOX_COLOR);

        highlightMatchingBlocks(poseStack, bufferSource, maid, maidPos, level, mainHandItem);

        if (TideFishingUtils.canFishInVoid(mainHandItem)) {
            int voidSurface = TideFishingUtils.getVoidSurface(level);
            BlockPos voidCenter = new BlockPos(maidPos.getX(), voidSurface, maidPos.getZ());
            drawSearchBox(poseStack, bufferSource, voidCenter, VOID_BOX_COLOR);
        }

        poseStack.popPose();

        drawDebugLabel(poseStack, bufferSource, cameraPos, maid);
    }

    private static void drawSearchBox(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                       BlockPos center, float[] color) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());

        int minX = center.getX() - 1;
        int minZ = center.getZ() - 1;
        int maxX = center.getX() + 1;
        int maxZ = center.getZ() + 1;
        int y = center.getY();

        Matrix4f matrix = poseStack.last().pose();
        float r = color[0], g = color[1], b = color[2];

        drawLine(vertexConsumer, matrix, minX, y, minZ, maxX, y, minZ, r, g, b);
        drawLine(vertexConsumer, matrix, maxX, y, minZ, maxX, y, maxZ, r, g, b);
        drawLine(vertexConsumer, matrix, maxX, y, maxZ, minX, y, maxZ, r, g, b);
        drawLine(vertexConsumer, matrix, minX, y, maxZ, minX, y, minZ, r, g, b);
    }

    private static void highlightMatchingBlocks(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                                  EntityMaid maid, BlockPos maidPos, Level level, ItemStack rod) {
        checkAndHighlight(poseStack, buffer, level, maidPos, rod);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = maidPos.offset(x, 0, z);
                checkAndHighlight(poseStack, buffer, level, checkPos, rod);
            }
        }

        checkAndHighlight(poseStack, buffer, level, maidPos.below(), rod);
    }

    private static void checkAndHighlight(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                           Level level, BlockPos pos, ItemStack rod) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        boolean isMatch = false;

        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            isMatch = true;
        } else if (level.getFluidState(pos).is(FluidTags.LAVA) && TideFishingUtils.canFishInLava(rod)) {
            isMatch = true;
        }

        if (isMatch) {
            LevelRenderer.renderLineBox(poseStack, vertexConsumer,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    MATCHED_BLOCK_COLOR[0], MATCHED_BLOCK_COLOR[1], MATCHED_BLOCK_COLOR[2], 1.0F);
        }
    }

    private static void drawDebugLabel(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                        Vec3 cameraPos, EntityMaid maid) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal("🎣 潮汐钓鱼搜索范围");

        double x = maid.getX() - cameraPos.x;
        double y = maid.getY() + maid.getBbHeight() + 0.5 - cameraPos.y;
        double z = maid.getZ() - cameraPos.z;

        double dist = Math.sqrt(x * x + y * y + z * z);
        float scale = (float) (dist * 0.025F);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        font.drawInBatch(text, -font.width(text) / 2.0F, 0, 0xFFFFFF, false,
                poseStack.last().pose(), buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        buffer.endLastBatch();

        poseStack.popPose();
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f matrix,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float r, float g, float b) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 1.0F).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 1.0F).setNormal(0, 1, 0);
    }
}
