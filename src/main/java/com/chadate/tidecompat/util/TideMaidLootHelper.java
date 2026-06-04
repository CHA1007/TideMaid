package com.chadate.tidecompat.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.registries.TideEntityTypes;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过代理 TideFishingHook 生成女仆 Tide 钓鱼战利品
 */
public final class TideMaidLootHelper {

    private TideMaidLootHelper() {
    }

    public static @NotNull List<ItemStack> rollTideLoot(
            EntityMaid maid,
            ServerLevel level,
            Vec3 hookPosition,
            ItemStack rod,
            int luckBonus,
            int lureSpeedBonus
    ) {
        if (!(maid.getOwner() instanceof ServerPlayer player)) {
            return List.of();
        }

        int luck = Math.max(0, luckBonus + Mth.floor(maid.getLuck()));
        int lureSpeed = Math.max(0, lureSpeedBonus);

        TideFishingHook proxy = new TideFishingHook(
                TideEntityTypes.FISHING_BOBBER, player, level, luck, lureSpeed, 0.0F, rod);
        proxy.moveTo(hookPosition);

        FishingHook previousHook = player.fishing;
        boolean added = false;
        try {
            if (!level.addFreshEntity(proxy)) {
                return List.of();
            }
            added = true;

            var fluidState = level.getFluidState(proxy.blockPosition());
            if (fluidState.is(net.minecraft.tags.FluidTags.WATER)
                    || fluidState.is(net.minecraft.tags.FluidTags.LAVA)) {
                TideReflectionHelper.setFluidState(proxy, fluidState);
            }

            FishingMedium medium = proxy.getCurrentMedium();
            if (medium != null) {
                TideReflectionHelper.setMedium(proxy, medium);
            }

            proxy.selectCatch(rod);
            return new ArrayList<>(proxy.getHookedItems());
        } finally {
            if (added && proxy.isAlive()) {
                proxy.discard();
            }
            if (previousHook != null && previousHook.isAlive() && player.fishing != previousHook) {
                player.fishing = previousHook;
            }
        }
    }
}
