package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class CrystalRodBehavior implements RodBehavior {

    @Override
    public void onNibble(TideMaidFishingHook hook, ServerLevel level) {
        level.playSound(
                null, hook.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 1.5f,
                1.0f - (hook.getRandom().nextFloat() - hook.getRandom().nextFloat()) * 0.1f);
    }
}
