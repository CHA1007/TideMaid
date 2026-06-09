package com.chadate.tidemaid.entity;

import com.chadate.tidemaid.init.TideEntities;
import com.chadate.tidemaid.task.TaskTideFishing;
import com.chadate.tidemaid.util.TideFishingUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.Tide;
import com.li64.tide.compat.seasons.SeasonsCompat;
import com.li64.tide.data.TideLootTables;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.FishingContext;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.data.rods.BaitContents;
import com.li64.tide.registries.TideItems;
import com.li64.tide.registries.TideParticleTypes;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import com.li64.tide.util.TideUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Tide 女仆鱼钩
 */
public class TideMaidFishingHook extends MaidFishingHook {
    private static final EntityDataAccessor<Float> DATA_INITIAL_YAW = SynchedEntityData
            .defineId(TideMaidFishingHook.class, EntityDataSerializers.FLOAT);

    private int particleTimer = 0;

    public TideMaidFishingHook(EntityType<TideMaidFishingHook> type, Level level) {
        super(type, level, 0, 0);
    }

    public TideMaidFishingHook(EntityMaid maid, Level level, int luck, int lureSpeed, Vec3 pos) {
        super(TideEntities.TIDE_MAID_FISHING_HOOK.get(), level, luck, lureSpeed);
        this.setOwner(maid);
        this.moveTo(pos);

        float yaw = maid.getYRot();
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.entityData.set(DATA_INITIAL_YAW, yaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_INITIAL_YAW, 0f);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected @NotNull List<ItemStack> getLoot(@NotNull MinecraftServer server, @NotNull LootParams lootParams) {
        EntityMaid maid = getMaidOwner();
        if (maid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return super.getLoot(server, lootParams);
        }

        var task = maid.getTask();
        if (!task.getUid().equals(TaskTideFishing.UID)) {
            return super.getLoot(server, lootParams);
        }

        ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
        List<ItemStack> tideLoot = rollTideLoot(serverLevel, rod);

        if (!tideLoot.isEmpty()) {
            if (rod.is(TideItems.DIAMOND_FISHING_ROD)) {
                serverLevel.addFreshEntity(new ExperienceOrb(
                        serverLevel, this.getX(), this.getY() + 0.5, this.getZ() + 0.5,
                        this.random.nextInt(4) + 1));
            }
            if (rod.is(TideItems.MIDAS_FISHING_ROD)) {
                LootTable bonusTable = TideLootTables.Fishing.BONUS_GOLD.getTable(server);
                if (bonusTable != LootTable.EMPTY) {
                    List<ItemStack> bonusItems = bonusTable.getRandomItems(lootParams);
                    tideLoot = new ArrayList<>(tideLoot);
                    tideLoot.addAll(bonusItems);
                }
            }
            return tideLoot;
        }

        return super.getLoot(server, lootParams);
    }

    @Override
    protected float getFluidHeight(net.minecraft.world.level.material.FluidState fluidState,
            @NotNull BlockPos blockPos) {
        EntityMaid maid = getMaidOwner();
        ItemStack rod = maid != null ? maid.getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY;
        boolean isTideTask = maid != null && maid.getTask().getUid().equals(TaskTideFishing.UID);

        if (fluidState.is(FluidTags.WATER)) {
            return fluidState.getHeight(this.level(), blockPos);
        }

        if (!isTideTask)
            return 0f;

        if (fluidState.is(FluidTags.LAVA) && TideFishingUtil.isLavaproof(rod)) {
            return fluidState.getHeight(this.level(), blockPos);
        }

        if (TideFishingUtil.isVoidproof(rod) && TideFishingUtil.isInVoid(this, this.level())) {
            return 0.5f;
        }
        return 0f;
    }

    @Override
    protected void fallTick(FluidState fluidState) {
        if (fluidState.is(FluidTags.WATER))
            return;

        if (fluidState.is(FluidTags.LAVA))
            return;

        if (TideFishingUtil.isVoidproof(
                getMaidOwner() != null ? getMaidOwner().getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY)) {
            if (TideFishingUtil.isInVoid(this, this.level()))
                return;
        }
        super.fallTick(fluidState);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            ItemStack rod = getMaidOwner() != null ? getMaidOwner().getItemInHand(InteractionHand.MAIN_HAND)
                    : ItemStack.EMPTY;
            if (rod.isEmpty())
                return;

            if (hasBaitInRod(rod, TideItems.LUCKY_BAIT)) {
                particleTimer++;
                if (particleTimer >= 5) {
                    this.level().addParticle(ParticleTypes.WAX_ON,
                            this.getRandomX(0.8),
                            this.getY((2.0 * this.random.nextDouble() - 1.0) * 0.4) + 0.2,
                            this.getRandomZ(0.8),
                            0.0, 0.0, 0.0);
                    particleTimer = 0;
                }
            }

            if (hasBaitInRod(rod, TideItems.MAGNETIC_BAIT)) {
                particleTimer++;
                if (particleTimer >= 3) {
                    Vec3 pos = this.position();
                    Vec3 randomPos = new Vec3(
                            pos.x() + 0.5 * (this.random.nextGaussian() - this.random.nextGaussian()),
                            pos.y() + 0.5 * (this.random.nextGaussian() - this.random.nextGaussian()),
                            pos.z() + 0.5 * (this.random.nextGaussian() - this.random.nextGaussian()));
                    Vec3 speed = pos.vectorTo(randomPos);
                    this.level().addParticle(ParticleTypes.OMINOUS_SPAWNING,
                            pos.x(), pos.y(), pos.z(),
                            speed.x(), speed.y(), speed.z());
                    particleTimer = 0;
                }
            }
        }
    }

    /**
     * 使用 Tide 的渔获系统计算渔获
     */
    private List<ItemStack> rollTideLoot(ServerLevel level, ItemStack rod) {
        EntityMaid maid = getMaidOwner();
        if (maid == null || maid.getOwner() == null) {
            return List.of();
        }

        FishingMedium currentMedium = detectCurrentMedium(level);
        BlockPos pos = this.blockPosition();

        TideFishingHook virtualHook = createVirtualHook(level, this.luck, rod);
        virtualHook.setPos(this.position());

        FishingContext context = new FishingContext(
                level,
                virtualHook,
                level.getRandom(),
                this.position(),
                pos,
                this.luck,
                currentMedium.id().getPath(),
                rod,
                level.getBiome(pos),
                TideUtils.findClosestNonWaterBiome(level, pos, 12, 2).orElse(level.getBiome(pos)),
                level.dimension(),
                Math.clamp(sampleTemperature(level, pos), -1.0f, 1.0f),
                level.getMoonPhase(),
                SeasonsCompat.getSeason(level));

        CatchResult result = Tide.FISHING_MANAGER.selectCatch(context);

        if (result.isEmpty()) {
            LootParams lootParams = context.createFishingLootParams();
            LootTable table = TideUtils.getLootTable(
                    BuiltInLootTables.FISHING_JUNK.location(), level.getServer());
            return table.getRandomItems(lootParams);
        }

        if (result.isFish()) {
            BaitContents baitContents = TideItemData.BAIT_CONTENTS.get(rod);
            if (baitContents != null && !baitContents.items().isEmpty()) {
                BaitContents.Mutable contents = new BaitContents.Mutable(baitContents);
                for (ItemStack bait : baitContents.items()) {
                    if (!bait.isEmpty()) {
                        contents.shrinkStack(bait);
                        break;
                    }
                }
                TideItemData.BAIT_CONTENTS.set(rod, contents.toImmutable());
            }
        }

        return new ArrayList<>(result.items());
    }

    /**
     * 使用反射创建虚拟 TideFishingHook 实例
     */
    private static TideFishingHook createVirtualHook(ServerLevel level, int luck, ItemStack rod) {
        try {
            var constructor = TideFishingHook.class.getDeclaredConstructor(
                    net.minecraft.world.entity.EntityType.class,
                    net.minecraft.world.level.Level.class,
                    int.class,
                    int.class,
                    net.minecraft.world.item.ItemStack.class);
            constructor.setAccessible(true);
            return constructor.newInstance(
                    com.li64.tide.registries.TideEntityTypes.FISHING_BOBBER,
                    level,
                    luck,
                    0,
                    rod);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create virtual TideFishingHook", e);
        }
    }

    /**
     * 采样位置温度
     */
    private float sampleTemperature(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getBaseTemperature();
    }

    /**
     * 检测浮漂当前所在的介质类型
     */
    private FishingMedium detectCurrentMedium(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        var fluidState = level.getFluidState(pos);

        if (fluidState.is(FluidTags.WATER)) {
            return FishingMedium.WATER;
        }

        if (fluidState.is(FluidTags.LAVA)) {
            return FishingMedium.LAVA;
        }

        if (TideFishingUtil.isVoidproof(
                getMaidOwner() != null ? getMaidOwner().getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY)) {
            if (TideFishingUtil.isInVoid(this, level)) {
                return FishingMedium.VOID;
            }
        }

        return FishingMedium.WATER;
    }

    @Override
    protected void spawnSplashParticle(@NotNull ServerLevel level, @NotNull BlockState blockState, double x, double y,
            double z) {
        FishingMedium medium = detectCurrentMedium(level);

        BlockPos particlePos = BlockPos.containing(x, y - 1.0, z);
        var particleFluidState = level.getFluidState(particlePos);

        if (medium == FishingMedium.WATER) {
            if (!particleFluidState.is(FluidTags.WATER))
                return;
            level.sendParticles(ParticleTypes.SPLASH, x, y, z, 2 + this.random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
        } else if (medium == FishingMedium.LAVA) {
            if (!particleFluidState.is(FluidTags.LAVA))
                return;
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 2 + this.random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
        } else if (medium == FishingMedium.VOID) {
            if (!TideFishingUtil.isInVoid(this, level))
                return;
            level.sendParticles(TideParticleTypes.VOID_RIPPLE_SMALL, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void spawnFishingParticle(@NotNull ServerLevel level, @NotNull BlockState blockState, double x, double y,
            double z, float sin, float cos) {
        FishingMedium medium = detectCurrentMedium(level);
        RandomSource random = this.random;

        BlockPos particlePos = BlockPos.containing(x, y - 1.0, z);
        var particleFluidState = level.getFluidState(particlePos);

        if (medium == FishingMedium.WATER) {
            if (!particleFluidState.is(FluidTags.WATER))
                return;

            if (random.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.BUBBLE, x, y - 0.1, z, 1, sin, 0.1, cos, 0.0);
            }
            float sinOffset = sin * 0.04f;
            float cosOffset = cos * 0.04f;
            level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, cosOffset, 0.01, -sinOffset, 1.0);
            level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, -cosOffset, 0.01, sinOffset, 1.0);
        } else if (medium == FishingMedium.LAVA) {
            if (!particleFluidState.is(FluidTags.LAVA))
                return;

            if (random.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.FLAME, x, y - 0.1, z, 1, sin, 0.1, cos, 0.0);
            }
            float sinOffset = sin * 0.04f;
            float cosOffset = cos * 0.04f;
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, cosOffset, 0.01, -sinOffset, 1.0);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 0, 0.0, 0.01, -sinOffset, 1.0);
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, -cosOffset, 0.01, sinOffset, 1.0);
        } else if (medium == FishingMedium.VOID) {
            if (!TideFishingUtil.isInVoid(this, level))
                return;

            if (random.nextFloat() < 0.35f) {
                level.sendParticles(TideParticleTypes.VOID_RIPPLE_SMALL, x, y - 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            float sinOffset = sin * 0.04f;
            float cosOffset = cos * 0.04f;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y - 0.1, z, 0, cosOffset, 0.0, -sinOffset, 1.0);
        }
    }

    @Override
    protected void spawnNibbleParticle(@NotNull ServerLevel level) {
        FishingMedium medium = detectCurrentMedium(level);
        double yOffset = this.getY() + 0.5;
        float bbWidth = this.getBbWidth();
        RandomSource random = this.random;

        BlockPos particlePos = BlockPos.containing(this.getX(), yOffset - 1.0, this.getZ());
        var particleFluidState = level.getFluidState(particlePos);

        ItemStack rod = getMaidOwner() != null ? getMaidOwner().getItemInHand(InteractionHand.MAIN_HAND)
                : ItemStack.EMPTY;

        if (medium == FishingMedium.WATER) {
            if (!particleFluidState.is(FluidTags.WATER))
                return;
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25f,
                    1.0f + (random.nextFloat() - random.nextFloat()) * 0.4f);
            level.sendParticles(ParticleTypes.BUBBLE, this.getX(), yOffset, this.getZ(), (int) (1.0f + bbWidth * 20.0f),
                    bbWidth, 0.0, bbWidth, 0.2f);
            level.sendParticles(ParticleTypes.FISHING, this.getX(), yOffset, this.getZ(),
                    (int) (1.0f + bbWidth * 20.0f), bbWidth, 0.0, bbWidth, 0.2f);
        } else if (medium == FishingMedium.LAVA) {
            if (!particleFluidState.is(FluidTags.LAVA))
                return;
            this.playSound(SoundEvents.BUCKET_EMPTY_LAVA, 0.25f,
                    1.0f + (random.nextFloat() - random.nextFloat()) * 0.4f);
            level.sendParticles(ParticleTypes.LAVA, this.getX(), yOffset, this.getZ(), (int) (1.0f + bbWidth * 20.0f),
                    bbWidth, 0.0, bbWidth, 0.2f);
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), yOffset, this.getZ(), (int) (1.0f + bbWidth * 20.0f),
                    bbWidth, 0.0, bbWidth, 0.2f);
        } else if (medium == FishingMedium.VOID) {
            if (!TideFishingUtil.isInVoid(this, level))
                return;
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25f,
                    1.0f + (random.nextFloat() - random.nextFloat()) * 0.4f);
            level.sendParticles(TideParticleTypes.VOID_RIPPLE_LARGE, this.getX(), yOffset - 0.5, this.getZ(), 0, 0.0,
                    0.0, 0.0, 0.2);
        } else {
            super.spawnNibbleParticle(level);
        }

        if (rod.is(TideItems.CRYSTAL_FISHING_ROD)) {
            this.level().playSound(
                    null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 1.5f,
                    1.0f - (this.random.nextFloat() - this.random.nextFloat()) * 0.1f);
        }
    }

    /**
     * 检查鱼竿是否装备了指定类型的鱼饵
     */
    private boolean hasBaitInRod(ItemStack rod, net.minecraft.world.item.Item baitItem) {
        BaitContents contents = TideItemData.BAIT_CONTENTS.get(rod);
        if (contents == null)
            return false;
        return contents.items().stream().anyMatch(stack -> stack.is(baitItem));
    }

}
