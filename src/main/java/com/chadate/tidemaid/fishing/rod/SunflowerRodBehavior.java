package com.chadate.tidemaid.fishing.rod;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SunflowerRodBehavior implements RodBehavior {

    @Override
    public void modifyStats(RodStats stats, Level level, EntityMaid maid, ItemStack rod) {
        boolean canSeeSky = level.canSeeSky(maid.blockPosition());
        boolean isSunny = level.isDay() && level.dimensionType().hasSkyLight() && !level.isRaining();
        if (canSeeSky && isSunny) {
            stats.addLuckBonus(1);
        }
    }
}
