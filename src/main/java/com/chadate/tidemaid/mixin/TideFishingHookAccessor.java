package com.chadate.tidemaid.mixin;

import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * TideFishingHook 的 Mixin Accessor，用于通过 @Invoker 调用私有构造函数
 */
@Mixin(value = TideFishingHook.class, remap = false)
public interface TideFishingHookAccessor {

    @Invoker("<init>")
    static TideFishingHook invokeInit(
            EntityType<?> type,
            Level level,
            int luck,
            int lureSpeed,
            ItemStack rod) {
        throw new AssertionError();
    }
}
