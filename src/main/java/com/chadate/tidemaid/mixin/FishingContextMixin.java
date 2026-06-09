package com.chadate.tidemaid.mixin;

import com.li64.tide.data.fishing.FishingContext;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FishingContext.class, remap = false)
public abstract class FishingContextMixin {

    @Shadow @Final @Nullable private TideFishingHook hook;
    @Shadow @Final private net.minecraft.server.level.ServerLevel level;
    @Shadow @Final private Vec3 pos;
    @Shadow @Final private int luck;

    @Inject(method = "createFishingLootParams", at = @At("HEAD"), cancellable = true)
    private void tidemaid$nullHookFallback(CallbackInfoReturnable<LootParams> cir) {
        if (this.hook == null) {
            LootParams lootParams = new LootParams.Builder(this.level)
                    .withParameter(LootContextParams.ORIGIN, this.pos)
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withLuck(this.luck)
                    .create(LootContextParamSets.FISHING);
            cir.setReturnValue(lootParams);
        }
    }
}
