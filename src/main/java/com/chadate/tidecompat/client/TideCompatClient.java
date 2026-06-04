package com.chadate.tidecompat.client;

import com.chadate.tidecompat.init.TideEntities;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.MaidFishingHookRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.client.TideItemModelProperties;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class TideCompatClient {

    /**
     * 注册实体渲染器（复用 TouhouLittleMaid 的浮漂渲染器）
     */
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TideEntities.TIDE_MAID_FISHING_HOOK.get(), MaidFishingHookRenderer::new);
    }

    public static void registerModelProperties() {
        ClampedItemPropertyFunction extendedCastFunction = (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;

            boolean isMainHand = entity.getMainHandItem() == stack;
            boolean isOffHand = entity.getOffhandItem() == stack;

            if (entity.getMainHandItem().getItem() instanceof FishingRodItem ||
                entity.getMainHandItem().getItem() instanceof TideFishingRodItem) {
                isOffHand = false;
            }

            if (entity instanceof Player player) {
                return (isMainHand || isOffHand) && player.fishing != null ? 1.0F : 0.0F;
            }
            else if (entity instanceof EntityMaid maid) {
                return (isMainHand || isOffHand) && maid.fishing != null ? 1.0F : 0.0F;
            }

            return 0.0F;
        };


            ItemProperties.register(TideItems.MIDAS_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.STONE_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.IRON_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.GOLDEN_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.CRYSTAL_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.DIAMOND_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
            ItemProperties.register(TideItems.NETHERITE_FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);


        ItemProperties.register(net.minecraft.world.item.Items.FISHING_ROD, TideItemModelProperties.CAST_PROPERTY, extendedCastFunction);
    }
}
