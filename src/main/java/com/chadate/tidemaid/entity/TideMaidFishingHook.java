package com.chadate.tidemaid.entity;

import com.chadate.tidemaid.data.MaidFishingData;
import com.chadate.tidemaid.entity.chatbubble.ItemChatBubbleData;
import com.chadate.tidemaid.fishing.rod.RodBehavior;
import com.chadate.tidemaid.fishing.rod.RodBehaviorRegistry;
import com.chadate.tidemaid.init.TideEntities;
import com.chadate.tidemaid.mixin.MaidFishingHookAccessor;
import com.chadate.tidemaid.mixin.TideFishingHookAccessor;
import com.chadate.tidemaid.task.TaskTideFishing;
import com.chadate.tidemaid.util.TideFishingUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleDataCollection;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.li64.tide.registries.TideEntityTypes;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import com.li64.tide.Tide;
import com.li64.tide.compat.seasons.SeasonsCompat;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.FishingContext;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.data.rods.BaitContents;
import com.li64.tide.data.rods.CustomRodManager;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    public static final EntityDataAccessor<ItemStack> DATA_PREVIEW_ITEM = SynchedEntityData
            .defineId(TideMaidFishingHook.class, EntityDataSerializers.ITEM_STACK);
    
    /**
     * 表示"无预览"的哨兵物品
     */
    private static final ItemStack NO_PREVIEW = new ItemStack(Items.STRUCTURE_VOID);
    
    /**
     * 检查预览物品是否为有效预览
     */
    public static boolean hasValidPreview(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.STRUCTURE_VOID);
    }

    private long chatBubbleKey = -1;

    private int particleTimer = 0;

    private CatchResult cachedCatchResult = null;

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
        builder.define(DATA_PREVIEW_ITEM, NO_PREVIEW);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    private void removeChatBubble() {
        EntityMaid maid = getMaidOwner();
        if (maid != null && chatBubbleKey >= 0) {
            maid.getChatBubbleManager().removeChatBubble(chatBubbleKey);
            chatBubbleKey = -1;
        }
    }

    public void showFishChatBubble(ItemStack fishStack) {
        EntityMaid maid = getMaidOwner();
        if (maid == null) return;
        
        clearAllChatBubbles();
        
        // 创建物品图标对话框显示鱼类，使用高优先级确保能替换其他对话框
        ItemChatBubbleData bubble = ItemChatBubbleData.create(200, 100, fishStack);
        chatBubbleKey = maid.getChatBubbleManager().addChatBubble(bubble);
    }

    /**
     * 设置预览物品
     */
    public void setPreviewItem(ItemStack preview) {
        this.entityData.set(DATA_PREVIEW_ITEM, preview);
    }

    /**
     * 清理预览物品
     */
    public void clearPreviewItem() {
        this.entityData.set(DATA_PREVIEW_ITEM, NO_PREVIEW);
    }

    /**
     * 检测浮漂当前所在的介质类型
     */
    public FishingMedium detectCurrentMedium(ServerLevel level) {
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

    /**
     * 烈焰鱼竿在岩浆中的特殊粒子效果
     */
    public void spawnBlazingLavaParticles() {
        if (!(this.level() instanceof ServerLevel level)) return;
        
        BlockPos particlePos = BlockPos.containing(this.getX(), this.getY() + 0.5, this.getZ());
        var particleFluidState = level.getFluidState(particlePos);
        
        if (!particleFluidState.is(FluidTags.LAVA)) return;
        
        RandomSource random = this.random;
        float bbWidth = this.getBbWidth();
        double yOffset = this.getY() + 0.5;
        
        if (random.nextFloat() < 0.15f) {
            level.sendParticles(ParticleTypes.FLAME, this.getX(), yOffset - 0.1, this.getZ(), 
                    1, 0.1, 0.1, 0.1, 0.0);
        }
        level.sendParticles(ParticleTypes.LAVA, this.getX(), yOffset, this.getZ(), 
                (int) (1.0f + bbWidth * 20.0f), bbWidth, 0.0, bbWidth, 0.2f);
        level.sendParticles(ParticleTypes.FLAME, this.getX(), yOffset, this.getZ(), 
                (int) (1.0f + bbWidth * 10.0f), bbWidth, 0.0, bbWidth, 0.1f);
    }

    private void clearAllChatBubbles() {
        EntityMaid maid = getMaidOwner();
        if (maid == null) return;
        
        ChatBubbleDataCollection collection = maid.getChatBubbleManager().getChatBubbleDataCollection();
        while (!collection.isEmpty()) {
            LongSortedSet keys = collection.keySet();
            if (!keys.isEmpty()) {
                long key = keys.firstLong();
                collection.remove(key);
            } else {
                break;
            }
        }
        maid.getChatBubbleManager().forceUpdateChatBubble();
        chatBubbleKey = -1;
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
        RodBehavior rodBehavior = RodBehaviorRegistry.getBehavior(rod);
        
        // 如果有缓存的渔获，直接使用缓存
        List<ItemStack> tideLoot;
        if (cachedCatchResult != null) {
            // 处理缓存的 CatchResult
            if (cachedCatchResult.isEmpty()) {
                // NOTHING 类型，返回原版钓鱼垃圾
                LootTable table = TideUtils.getLootTable(
                        BuiltInLootTables.FISHING_JUNK.location(), serverLevel.getServer());
                tideLoot = table.getRandomItems(lootParams);
            } else {
                // FISH/ITEM/CRATE 类型，返回物品列表
                tideLoot = new ArrayList<>(cachedCatchResult.items());
                
                // 使用策略模式添加额外奖励
                rodBehavior.addBonusLoot(this, serverLevel, tideLoot, lootParams, cachedCatchResult);
            }
            cachedCatchResult = null; // 清除缓存
        } else {
            // 没有缓存，正常 roll
            tideLoot = rollTideLoot(serverLevel, rod);
        }

        if (!tideLoot.isEmpty()) {
            if (maid.getOwner() instanceof ServerPlayer serverPlayer) {
                for (ItemStack stack : tideLoot) {
                    if (TideUtils.isJournalFish(stack)) {
                        MaidFishingData data = MaidFishingData.getOrCreate(serverPlayer, maid.getUUID());
                        data.logCatch(stack, serverLevel);
                        data.syncTo(serverPlayer);
                    }
                }
            }

            // 使用策略模式处理钓到鱼时的效果
            CatchResult finalResult = cachedCatchResult != null ? cachedCatchResult : CatchResult.empty();
            rodBehavior.onCatch(this, finalResult);

            return tideLoot;
        }

        return super.getLoot(server, lootParams);
    }

    @Override
    protected float getFluidHeight(FluidState fluidState,
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

    private boolean wasBiting = false;

    @Override
    public void tick() {
        super.tick();

        boolean currentBiting = ((MaidFishingHookAccessor) this).isBiting();
        if (!this.level().isClientSide && currentBiting && !wasBiting) {
            EntityMaid maid = getMaidOwner();
            if (maid != null && this.level() instanceof ServerLevel serverLevel) {
                ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
                RodBehavior rodBehavior = RodBehaviorRegistry.getBehavior(rod);
                
                CatchResult catchResult = rollTideCatchResult(serverLevel, rod);
                
                cachedCatchResult = catchResult;
                
                rodBehavior.onCatch(this, catchResult);
            }
        }
        // 当咬钩状态结束时，移除对话框和预览
        if (!this.level().isClientSide && !currentBiting && wasBiting) {
            removeChatBubble();
            EntityMaid maid = getMaidOwner();
            if (maid != null) {
                ItemStack rod = maid.getItemInHand(InteractionHand.MAIN_HAND);
                RodBehavior rodBehavior = RodBehaviorRegistry.getBehavior(rod);
                rodBehavior.onRetrieve(this);
            }
        }
        wasBiting = currentBiting;

        if (this.level().isClientSide) {
            ItemStack rod = getMaidOwner() != null ? getMaidOwner().getItemInHand(InteractionHand.MAIN_HAND)
                    : ItemStack.EMPTY;
            if (rod.isEmpty())
                return;

            if (TideFishingUtil.hasLuckBait(rod)) {
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

            if (TideFishingUtil.hasCrateBait(rod)) {
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
     * 使用 Tide 的渔获系统计算渔获，返回 CatchResult 以便判断类型
     */
    private CatchResult rollTideCatchResult(ServerLevel level, ItemStack rod) {
        EntityMaid maid = getMaidOwner();
        if (maid == null || maid.getOwner() == null) {
            return CatchResult.empty();
        }

        FishingMedium currentMedium = detectCurrentMedium(level);
        BlockPos pos = this.blockPosition();

        TideFishingHook virtualHook = createVirtualHook(level, this.luck, rod);
        virtualHook.setPos(this.position());

        // 获取钓鱼维度
        RodBehavior rodBehavior = RodBehaviorRegistry.getBehavior(rod);
        var fishingDimension = rodBehavior.getFishingDimension(this, level, currentMedium);
        if (fishingDimension == null) {
            fishingDimension = level.dimension();
        }

        // 获取鱼钩温度修正（FIERY_HOOK: +0.25, PERMAFROST_HOOK: -0.25）
        float temperatureBonus = getHookTemperatureBonus(rod);

        FishingContext context = new FishingContext(
                level,
                virtualHook,
                rod,
                level.getRandom(),
                this.position(),
                pos,
                this.luck,
                currentMedium.id().getPath(),
                level.getBiome(pos),
                TideUtils.findClosestNonWaterBiome(level, pos, 12, 2).orElse(level.getBiome(pos)),
                fishingDimension,
                Math.clamp(sampleTemperature(level, pos) + temperatureBonus, -1.0f, 1.0f),
                level.getMoonPhase(),
                SeasonsCompat.getSeason(level));

        CatchResult result = Tide.FISHING_MANAGER.selectCatch(context);

        // 仅对鱼类物品消耗鱼饵
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

        return result;
    }

    /**
     * 使用 Tide 的渔获系统计算渔获
     */
    private List<ItemStack> rollTideLoot(ServerLevel level, ItemStack rod) {
        CatchResult result = rollTideCatchResult(level, rod);

        if (result.isEmpty()) {
            FishingMedium currentMedium = detectCurrentMedium(level);
            BlockPos pos = this.blockPosition();
            TideFishingHook virtualHook = createVirtualHook(level, this.luck, rod);
            virtualHook.setPos(this.position());
            boolean netherOverride = rod.is(TideItems.BLAZING_FISHING_ROD) && currentMedium == FishingMedium.LAVA;
            var fishingDimension = netherOverride ? Level.NETHER : level.dimension();
            
            float temperatureBonus = getHookTemperatureBonus(rod);
            
            FishingContext context = new FishingContext(
                    level,
                    virtualHook,
                    rod,
                    level.getRandom(),
                    this.position(),
                    pos,
                    this.luck,
                    currentMedium.id().getPath(),
                    level.getBiome(pos),
                    TideUtils.findClosestNonWaterBiome(level, pos, 12, 2).orElse(level.getBiome(pos)),
                    fishingDimension,
                    Math.clamp(sampleTemperature(level, pos) + temperatureBonus, -1.0f, 1.0f),
                    level.getMoonPhase(),
                    SeasonsCompat.getSeason(level));
            LootParams lootParams = context.createFishingLootParams();
            LootTable table = TideUtils.getLootTable(
                    BuiltInLootTables.FISHING_JUNK.location(), level.getServer());
            return table.getRandomItems(lootParams);
        }

        return new ArrayList<>(result.items());
    }

    /**
     * 创建虚拟 TideFishingHook 实例
     */
    private static TideFishingHook createVirtualHook(ServerLevel level, int luck, ItemStack rod) {
        return TideFishingHookAccessor.invokeInit(
                TideEntityTypes.FISHING_BOBBER,
                level,
                luck,
                0,
                rod);
    }

    /**
     * 采样位置温度
     */
    private float sampleTemperature(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getBaseTemperature();
    }

    /**
     * 获取鱼钩的温度修正值
     * FIERY_HOOK: +0.25
     * PERMAFROST_HOOK: -0.25
     */
    private float getHookTemperatureBonus(ItemStack rod) {
        ItemStack hook = CustomRodManager.getHook(rod);
        if (hook.is(TideItems.FIERY_HOOK)) return 0.25f;
        if (hook.is(TideItems.PERMAFROST_HOOK)) return -0.25f;
        return 0f;
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

        // 处理咬钩时的音效
        RodBehavior rodBehavior = RodBehaviorRegistry.getBehavior(rod);
        rodBehavior.onNibble(this, level);
    }

}
