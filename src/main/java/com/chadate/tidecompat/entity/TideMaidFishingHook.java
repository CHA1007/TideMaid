package com.chadate.tidecompat.entity;

import com.chadate.tidecompat.init.TideEntities;
import com.chadate.tidecompat.task.TaskTideFishing;
import com.chadate.tidecompat.util.TideFishingUtils;
import com.chadate.tidecompat.util.TideMaidLootHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.TideParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Tide 兼容的女仆钓鱼钩，模拟 Tide FishingMedium 介质系统
 */
public class TideMaidFishingHook extends MaidFishingHook {

    private boolean crystalSoundPlayed = false;

    public TideMaidFishingHook(EntityType<TideMaidFishingHook> type, Level level) {
        super(type, level, 0, 0);
    }

    public TideMaidFishingHook(EntityMaid maid, Level level, int luck, int lureSpeed, Vec3 pos) {
        super(TideEntities.TIDE_MAID_FISHING_HOOK.get(), level, luck, lureSpeed);
        this.setOwner(maid);
        this.moveTo(pos);
    }

    private ItemStack getRodItem() {
        EntityMaid maid = getMaidOwner();
        if (maid == null) return ItemStack.EMPTY;
        return maid.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private boolean isTideFishingTask() {
        EntityMaid maid = getMaidOwner();
        if (maid == null) return false;
        return maid.getTask().getUid().equals(TaskTideFishing.UID);
    }

    /**
     * 模拟 Tide FishingMedium.getHeight()，返回介质浸入高度
     */
    private float getMediumHeight(BlockPos blockPos, FluidState fluidState) {
        if (fluidState.is(FluidTags.WATER)) {
            return fluidState.getHeight(this.level(), blockPos);
        }
        if (fluidState.is(FluidTags.LAVA) && isTideFishingTask()) {
            return fluidState.getHeight(this.level(), blockPos);
        }
        if (isTideFishingTask() && TideFishingUtils.canFishInVoid(getRodItem())) {
            int voidSurface = TideFishingUtils.getVoidSurface(this.level());
            double depthBelowSurface = voidSurface - this.getY();
            if (depthBelowSurface > 0) {
                return (float) Math.min(depthBelowSurface, 1.0);
            }
        }
        return 0f;
    }

    private boolean isInVoid() {
        if (!isTideFishingTask()) return false;
        if (!TideFishingUtils.canFishInVoid(getRodItem())) return false;
        return this.getY() <= TideFishingUtils.getVoidSurface(this.level());
    }

    private boolean isLavaMedium() {
        return this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA);
    }

    @Override
    protected void fishingTick() {
        BlockPos blockPos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(blockPos);
        float mediumHeight = getMediumHeight(blockPos, fluidState);
        boolean inMedium = mediumHeight > 0.0f;

        if (this.currentState == FishHookState.FLYING) {
            if (inMedium) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                this.currentState = FishHookState.BOBBING;
                return;
            }
        } else if (this.currentState == FishHookState.BOBBING) {
            this.bobbingTick(blockPos, mediumHeight);
            this.checkOpenWater(blockPos);

            if (inMedium) {
                if (!crystalSoundPlayed && getRodItem().is(TideItems.CRYSTAL_FISHING_ROD)) {
                    this.level().playSound(null, this.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.MASTER, 1.5F,
                            1.0F - (this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    crystalSoundPlayed = true;
                }
                this.bitingTick(blockPos);
            } else {
                this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
            }
        }

        this.fallTick(fluidState);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();

        if (this.currentState == FishHookState.FLYING && (this.onGround() || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
        this.reapplyPosition();
    }

    @Override
    protected void spawnSplashParticle(ServerLevel level, BlockState blockState,
                                       double x, double y, double z) {
        if (isInVoid()) {
            level.sendParticles(TideParticleTypes.VOID_RIPPLE_SMALL, x, y, z, 0, 0.0, 0.0, 0.0, 0.0);
        } else if (isLavaMedium() && blockState.getFluidState().is(FluidTags.LAVA)) {
            level.sendParticles(ParticleTypes.LAVA, x, y, z,
                    2 + this.random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
        } else if (blockState.is(Blocks.WATER)) {
            level.sendParticles(ParticleTypes.SPLASH, x, y, z,
                    2 + this.random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
        }
    }

    @Override
    protected void spawnNibbleParticle(ServerLevel level) {
        RandomSource random = this.random;

        if (isInVoid()) {
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F,
                    1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);
            level.sendParticles(TideParticleTypes.VOID_RIPPLE_LARGE,
                    this.getX(), this.getY() - 0.5, this.getZ(), 0, 0.0, 0.0, 0.0, 0.2);
        } else if (isLavaMedium()) {
            this.playSound(SoundEvents.BUCKET_EMPTY_LAVA, 0.25F,
                    1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);

            double yOffset = this.getY() + 0.5D;
            float bbWidth = this.getBbWidth();
            level.sendParticles(ParticleTypes.LAVA, this.getX(), yOffset, this.getZ(),
                    (int) (1.0F + bbWidth * 20.0F), bbWidth, 0.0, bbWidth, 0.2F);
            level.sendParticles(ParticleTypes.FLAME, this.getX(), yOffset, this.getZ(),
                    (int) (1.0F + bbWidth * 20.0F), bbWidth, 0.0, bbWidth, 0.2F);
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), yOffset, this.getZ(),
                    (int) (1.0F + bbWidth * 20.0F), bbWidth, 0.0, bbWidth, 0.2F);
        } else {
            super.spawnNibbleParticle(level);
        }
    }

    @Override
    protected void spawnFishingParticle(ServerLevel level, BlockState blockState,
                                        double x, double y, double z, float sin, float cos) {
        RandomSource random = this.random;

        if (isInVoid()) {
            if (random.nextFloat() < 0.35F) {
                level.sendParticles(TideParticleTypes.VOID_RIPPLE_SMALL,
                        x, y - 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            float scaledSin = sin * 0.04F;
            float scaledCos = cos * 0.04F;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y - 0.1, z, 0, scaledCos, 0.0, -scaledSin, 1.0);
        } else if (isLavaMedium() && blockState.getFluidState().is(FluidTags.LAVA)) {
            if (random.nextFloat() < 0.15F) {
                level.sendParticles(ParticleTypes.FLAME, x, y - 0.1, z, 1, sin, 0.1, cos, 0.0);
            }

            float scaledSin = sin * 0.04F;
            float scaledCos = cos * 0.04F;
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, scaledCos, 0.01, -scaledSin, 1.0);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 0, 0, 0.01, -scaledSin, 1.0);
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, -scaledCos, 0.01, scaledSin, 1.0);
        } else if (blockState.is(Blocks.WATER)) {
            super.spawnFishingParticle(level, blockState, x, y, z, sin, cos);
        }
    }

    @Override
    protected void fallTick(FluidState fluidState) {
        if (!fluidState.is(FluidTags.WATER) && !fluidState.is(FluidTags.LAVA) && !isInVoid()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }
    }

    @Override
    protected float getFluidHeight(FluidState fluidState, @NotNull BlockPos blockPos) {
        return getMediumHeight(blockPos, fluidState);
    }

    @Override
    protected @NotNull List<ItemStack> getLoot(MinecraftServer server, LootParams lootParams) {
        EntityMaid maid = getMaidOwner();
        if (maid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return super.getLoot(server, lootParams);
        }

        if (!isTideFishingTask()) {
            return super.getLoot(server, lootParams);
        }

        ItemStack rod = maid.getMainHandItem();
        List<ItemStack> tideLoot = TideMaidLootHelper.rollTideLoot(
                maid, serverLevel, this.position(), rod, this.luck, this.lureSpeed);

        if (!tideLoot.isEmpty()) {
            if (rod.is(TideItems.DIAMOND_FISHING_ROD)) {
                serverLevel.addFreshEntity(new ExperienceOrb(
                        serverLevel, this.getX(), this.getY() + 0.5, this.getZ() + 0.5,
                        this.random.nextInt(4) + 1));
            }
            return tideLoot;
        }
        return super.getLoot(server, lootParams);
    }
}
