package com.chadate.tidecompat.util;

import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Tide 反射工具类，集中管理对 Tide 私有字段的访问
 */
public final class TideReflectionHelper {

    @Nullable
    private static Field fluidStateField;
    @Nullable
    private static Field mediumField;

    private TideReflectionHelper() {
    }

    public static void setFluidState(TideFishingHook hook, FluidState state) {
        if (fluidStateField == null) {
            fluidStateField = findField("fluidState");
        }
        setField(fluidStateField, hook, state);
    }

    public static void setMedium(TideFishingHook hook, @Nullable FishingMedium medium) {
        if (mediumField == null) {
            mediumField = findField("medium");
        }
        setField(mediumField, hook, medium);
    }

    @Nullable
    private static Field findField(String fieldName) {
        try {
            Field field = TideFishingHook.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static void setField(@Nullable Field field, Object target, Object value) {
        if (field != null) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
