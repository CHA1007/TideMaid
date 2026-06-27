package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.util.TideUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class VillageRodBehavior implements RodBehavior {

    @Override
    public void addBonusLoot(TideMaidFishingHook hook, ServerLevel level, List<ItemStack> loot,
                             LootParams lootParams, CatchResult result) {
        if (!result.isEmpty()) {
            FishingMedium currentMedium = hook.detectCurrentMedium(level);
            if (currentMedium != FishingMedium.LAVA) {
                ItemStack bonus = TideUtils.getBonusVillageItem(level.getServer(), lootParams);
                if (bonus != null) {
                    loot.add(bonus);
                }
            }
        }
    }
}
