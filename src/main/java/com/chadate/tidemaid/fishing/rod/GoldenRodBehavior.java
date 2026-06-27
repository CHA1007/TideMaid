package com.chadate.tidemaid.fishing.rod;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GoldenRodBehavior implements RodBehavior {

    @Override
    public void modifyStats(RodStats stats, Level level, EntityMaid maid, ItemStack rod) {
        stats.addLuckBonus(1);
    }
}
