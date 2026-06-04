package com.chadate.tidecompat.task;

import com.chadate.tidecompat.TideTouhoulittlemaidCompat;
import com.chadate.tidecompat.ai.TideMaidAdvancedFishingAI;
import com.github.tartaricacid.touhoulittlemaid.api.task.FunctionCallSwitchResult;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ride.MaidRideFindWaterTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFindSitTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskFishing;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.TaskEquipUtil;
import com.google.common.collect.Lists;
import com.li64.tide.registries.TideItems;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TaskTideFishing extends TaskFishing {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TideTouhoulittlemaidCompat.MOD_ID, "tide_fishing");

    @Override
    public @NotNull ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @NotNull ItemStack getIcon() {
        return TideItems.MIDAS_FISHING_ROD.getDefaultInstance();
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound(@NotNull EntityMaid maid) {
        return InitSounds.MAID_IDLE.get();
    }

    @Override
    public @NotNull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(@NotNull EntityMaid maid) {
        return Lists.newArrayList(
                Pair.of(5, new MaidFindSitTask(0.6f)),
                Pair.of(6, TideMaidAdvancedFishingAI.create())
        );
    }

    @Override
    public @NotNull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(@NotNull EntityMaid maid) {
        return Lists.newArrayList(Pair.of(5, new MaidRideFindWaterTask(6, 3)));
    }

    @Override
    public @NotNull FunctionCallSwitchResult onFunctionCallSwitch(EntityMaid maid) {
        if (maid.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
            return FunctionCallSwitchResult.NO_CHANGE;
        }
        if (TaskEquipUtil.tryEquipFromBackpack(maid, item -> item.canPerformAction(ItemAbilities.FISHING_ROD_CAST))) {
            return FunctionCallSwitchResult.OK;
        }
        return FunctionCallSwitchResult.MISSING_REQUIRED_ITEM;
    }

    @Override
    public @NotNull String getMaidActionSummary() {
        return "Use fishing rod to fish with Tide mechanics";
    }
}