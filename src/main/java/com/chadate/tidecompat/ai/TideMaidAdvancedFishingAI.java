package com.chadate.tidecompat.ai;

import com.chadate.tidecompat.entity.TideMaidFishingHook;
import com.chadate.tidecompat.task.TaskTideFishing;
import com.chadate.tidecompat.util.TideFishingUtils;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tide 女仆钓鱼 AI 行为
 */
public class TideMaidAdvancedFishingAI {

    public static BehaviorControl<EntityMaid> create() {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.absent(MemoryModuleType.LOOK_TARGET),
                instance.absent(MemoryModuleType.WALK_TARGET)).apply(instance, (lookTarget, walkTarget) -> {
                    return (world, maid, time) -> {
                        if (!isTideFishingTask(maid)) {
                            return false;
                        }

                        ItemStack mainHandItem = maid.getItemInHand(InteractionHand.MAIN_HAND);
                        if (!(mainHandItem.getItem() instanceof TideFishingRodItem) &&
                                isTideMidasRod(mainHandItem)) {
                            return false;
                        }

                        if (mainHandItem.getMaxDamage() > 0
                                && mainHandItem.getDamageValue() >= mainHandItem.getMaxDamage()) {
                            return false;
                        }

                        if (!maid.hasFishingHook()) {
                            BlockPos waterPos = findWaterAtMaidPosition(maid, maid.blockPosition());
                            if (waterPos != null && performCastRod(maid, waterPos)) {
                                if (!maid.level().isClientSide()) {
                                    maid.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0));
                                }
                                return true;
                            }
                        }
                        return true;
                    };
                }));
    }

    private static boolean isTideMidasRod(ItemStack stack) {
        return !stack.is(TideItems.MIDAS_FISHING_ROD);
    }

    private static boolean isTideFishingTask(EntityMaid maid) {
        return maid.getTask().getUid().equals(TaskTideFishing.UID);
    }

    private static BlockPos findWaterAtMaidPosition(EntityMaid maid, BlockPos maidPos) {
        Level level = maid.level();
        ItemStack mainHandItem = maid.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(mainHandItem.getItem() instanceof TideFishingRodItem) && isTideMidasRod(mainHandItem)) {
            return null;
        }

        if (level.getFluidState(maidPos).is(FluidTags.WATER)) {
            return maidPos;
        }

        if (level.getFluidState(maidPos).is(FluidTags.LAVA) && TideFishingUtils.canFishInLava(mainHandItem)) {
            return maidPos;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = maidPos.offset(x, 0, z);
                BlockPos abovePos = checkPos.above();
                if (!level.isEmptyBlock(abovePos)) continue;

                if (level.getFluidState(checkPos).is(FluidTags.WATER)) {
                    return checkPos;
                }

                if (level.getFluidState(checkPos).is(FluidTags.LAVA) && TideFishingUtils.canFishInLava(mainHandItem)) {
                    return checkPos;
                }
            }
        }

        BlockPos belowPos = maidPos.below();
        if (level.getFluidState(belowPos).is(FluidTags.WATER)) {
            return belowPos;
        }

        if (level.getFluidState(belowPos).is(FluidTags.LAVA) && TideFishingUtils.canFishInLava(mainHandItem)) {
            return belowPos;
        }

        if (TideFishingUtils.canFishInVoid(mainHandItem)) {
            return findVoidFishingPosition(level, maid, maidPos);
        }

        return null;
    }

    private static BlockPos findVoidFishingPosition(Level level, EntityMaid maid, BlockPos maidPos) {
        int voidSurface = TideFishingUtils.getVoidSurface(level);
        float yaw = maid.getYHeadRot();
        int forwardX = (int) Math.round(-Mth.sin(yaw * ((float)Math.PI / 180F)) * 2.5F);
        int forwardZ = (int) Math.round(Mth.cos(yaw * ((float)Math.PI / 180F)) * 2.5F);

        BlockPos preferredPos = new BlockPos(maidPos.getX() + forwardX, voidSurface, maidPos.getZ() + forwardZ);
        if (level.isEmptyBlock(preferredPos)) {
            return preferredPos;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = new BlockPos(maidPos.getX() + x, voidSurface, maidPos.getZ() + z);
                if (level.isEmptyBlock(checkPos)) {
                    return checkPos;
                }
            }
        }

        BlockPos fallbackPos = new BlockPos(maidPos.getX(), voidSurface, maidPos.getZ());
        return level.isEmptyBlock(fallbackPos) ? fallbackPos : null;
    }

    private static boolean performCastRod(EntityMaid maid, BlockPos waterPos) {
        Level world = maid.level();
        ItemStack mainHandItem = maid.getItemInHand(InteractionHand.MAIN_HAND);

        int luck = EnchantmentHelper.getFishingLuckBonus((ServerLevel) world, mainHandItem, maid);
        int speed = (int) (EnchantmentHelper.getFishingTimeReduction((ServerLevel) world, mainHandItem, maid) / 5f);

        if (mainHandItem.getItem() == TideItems.GOLDEN_FISHING_ROD
                || mainHandItem.getItem() == TideItems.MIDAS_FISHING_ROD) {
            luck += 1;
        }

        int lureSpeedBonus = speed * 20;
        Vec3 spawnPos = TideFishingUtils.getBobberSpawnPosition(world, waterPos);

        MaidFishingHook fishingHook = new TideMaidFishingHook(maid, world, luck, lureSpeedBonus, spawnPos);
        world.addFreshEntity(fishingHook);

        maid.swing(InteractionHand.MAIN_HAND);
        maid.getLookControl().setLookAt(fishingHook);

        return true;
    }
}
