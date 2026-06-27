package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.data.TideLootTables;
import com.li64.tide.data.fishing.CatchResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public class MidasRodBehavior implements RodBehavior {

    @Override
    public void addBonusLoot(TideMaidFishingHook hook, ServerLevel level, List<ItemStack> loot,
                             LootParams lootParams, CatchResult result) {
        if (!result.isEmpty()) {
            LootTable bonusTable = TideLootTables.Fishing.BONUS_GOLD.getTable(level.getServer());
            if (bonusTable != LootTable.EMPTY) {
                List<ItemStack> bonusItems = bonusTable.getRandomItems(lootParams);
                loot.addAll(bonusItems);
            }
        }
    }
}
