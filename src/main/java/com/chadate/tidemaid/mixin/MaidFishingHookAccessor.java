package com.chadate.tidemaid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问咬钩状态
 */
@Mixin(value = MaidFishingHook.class)
public interface MaidFishingHookAccessor {

    @Accessor("biting")
    boolean isBiting();
}