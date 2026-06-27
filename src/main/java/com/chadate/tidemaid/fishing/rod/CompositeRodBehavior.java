package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompositeRodBehavior implements RodBehavior {

    private final RodBehavior[] behaviors;

    public CompositeRodBehavior(RodBehavior... behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void modifyStats(RodStats stats, Level level, EntityMaid maid, ItemStack rod) {
        for (RodBehavior behavior : behaviors) {
            behavior.modifyStats(stats, level, maid, rod);
        }
    }

    @Override
    public void onNibble(TideMaidFishingHook hook, ServerLevel level) {
        for (RodBehavior behavior : behaviors) {
            behavior.onNibble(hook, level);
        }
    }

    @Override
    public boolean onWaitParticle(TideMaidFishingHook hook, ServerLevel level, FishingMedium medium) {
        for (RodBehavior behavior : behaviors) {
            if (behavior.onWaitParticle(hook, level, medium)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public ResourceKey<Level> getFishingDimension(TideMaidFishingHook hook, ServerLevel level, FishingMedium medium) {
        for (RodBehavior behavior : behaviors) {
            ResourceKey<Level> dimension = behavior.getFishingDimension(hook, level, medium);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }

    @Override
    public void addBonusLoot(TideMaidFishingHook hook, ServerLevel level, List<ItemStack> loot,
                             LootParams lootParams, CatchResult result) {
        for (RodBehavior behavior : behaviors) {
            behavior.addBonusLoot(hook, level, loot, lootParams, result);
        }
    }

    @Override
    public void onCatch(TideMaidFishingHook hook, CatchResult result) {
        for (RodBehavior behavior : behaviors) {
            behavior.onCatch(hook, result);
        }
    }

    @Override
    public void onRetrieve(TideMaidFishingHook hook) {
        for (RodBehavior behavior : behaviors) {
            behavior.onRetrieve(hook);
        }
    }
}
