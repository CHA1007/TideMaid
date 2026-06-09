package com.chadate.tidemaid.client.model;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.Tide;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.jetbrains.annotations.NotNull;

/**
 * Tide 浮漂模型
 */
public class TideFishingModel<T extends TideMaidFishingHook> extends EntityModel<T> {
    public static final ModelLayerLocation MODEL_LOCATION = new ModelLayerLocation(Tide.resource("fishing_hook"), "main");
    private final ModelPart bobber;
    private final ModelPart top;
    private final ModelPart top2;
    private final ModelPart hook;

    public TideFishingModel(ModelPart root) {
        this.bobber = root.getChild("bobber");
        this.top = root.getChild("top");
        this.top2 = root.getChild("top2");
        this.hook = root.getChild("hook");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild("bobber", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 3.0F,
                        new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        partDefinition.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(5, 7).addBox(0.0F, -4.0F, 0.5F, 1.0F, 1.0F, 0.0F,
                        new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        partDefinition.addOrReplaceChild("top2", CubeListBuilder.create()
                .texOffs(5, 7).addBox(-0.5F, -4.0F, 0.0F, 1.0F, 1.0F, 0.0F,
                        new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.0F, 0.5F, 0.0F, -1.5708F, 0.0F));

        partDefinition.addOrReplaceChild("hook", CubeListBuilder.create()
                .texOffs(3, 7).addBox(-2.0F, 0.0F, 0.4F, 3.0F, 3.0F, 0.0F,
                        new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    @Override
    public void setupAnim(@NotNull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        bobber.render(poseStack, buffer, packedLight, packedOverlay, color);
        top.render(poseStack, buffer, packedLight, packedOverlay, color);
        top2.render(poseStack, buffer, packedLight, packedOverlay, color);
        hook.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /**
     * 获取浮漂主体部分
     */
    public ModelPart getBobber() {
        return bobber;
    }

    /**
     * 获取顶部装饰 1
     */
    public ModelPart getTop() {
        return top;
    }

    /**
     * 获取顶部装饰 2
     */
    public ModelPart getTop2() {
        return top2;
    }

    /**
     * 获取鱼钩部分
     */
    public ModelPart getHook() {
        return hook;
    }
}
