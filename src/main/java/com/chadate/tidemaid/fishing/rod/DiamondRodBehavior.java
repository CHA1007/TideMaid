package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.data.fishing.CatchResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class DiamondRodBehavior implements RodBehavior {

    @Override
    public void addBonusLoot(TideMaidFishingHook hook, ServerLevel level, List<ItemStack> loot,
                             LootParams lootParams, CatchResult result) {
        if (!result.isEmpty()) {
            level.addFreshEntity(new ExperienceOrb(
                    level, hook.getX(), hook.getY() + 0.5, hook.getZ() + 0.5,
                    hook.getRandom().nextInt(4) + 1));
        }
    }
}
