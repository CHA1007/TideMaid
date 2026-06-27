package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BlazingRodBehavior implements RodBehavior {

    @Override
    @Nullable
    public ResourceKey<Level> getFishingDimension(TideMaidFishingHook hook, ServerLevel level, FishingMedium medium) {
        if (medium == FishingMedium.LAVA) {
            return Level.NETHER;
        }
        return null;
    }
}
