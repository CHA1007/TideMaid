package com.chadate.tidemaid.data;

import com.chadate.tidemaid.network.SyncMaidFishingDataMsg;
import com.li64.tide.data.fishing.FishData;
import com.li64.tide.data.item.TideItemData;
import com.li64.tide.data.player.CatchTimestamp;
import com.li64.tide.data.player.FishStats;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 女仆钓鱼数据存储
 * 数据存储在玩家身上，按女仆 UUID 区分每个女仆的钓鱼记录
 */
public class MaidFishingData {
    /** 客户端缓存：女仆 UUID -> 女仆钓鱼数据 */
    public static final Map<UUID, MaidFishingData> CLIENT_DATA = new HashMap<>();
    
    /** 服务端缓存：(玩家UUID, 女仆UUID) -> 女仆钓鱼数据 */
    private static final Map<String, MaidFishingData> SERVER_CACHE = new HashMap<>();

    /** 客户端数据同步回调 */
    @Nullable
    private static Consumer<UUID> dataSyncCallback = null;

    /**
     * 注册数据同步回调
     */
    public static void setDataSyncCallback(Consumer<UUID> callback) {
        dataSyncCallback = callback;
    }

    /**
     * 通知客户端数据已同步
     */
    public static void notifyDataSync(UUID maidUuid) {
        if (dataSyncCallback != null) {
            dataSyncCallback.accept(maidUuid);
        }
    }

    private static final String NBT_TAG = "TideMaidFishingData";

    /** 女仆的 UUID */
    private final UUID maidUuid;
    /** 每种鱼的钓鱼数据（解锁状态、阅读状态、笔记状态等） */
    public Map<Holder<Item>, MaidFishData> fishData;

    @SuppressWarnings("unused")
    public MaidFishingData() {
        this(UUID.randomUUID());
    }

    public MaidFishingData(UUID maidUuid) {
        this(maidUuid, new CompoundTag());
    }

    public MaidFishingData(UUID maidUuid, CompoundTag tag) {
        this.maidUuid = maidUuid;
        this.fishData = new HashMap<>();

        ListTag fishList = tag.getList("fish_data", 10);
        fishList.forEach(t -> {
            if (!(t instanceof CompoundTag fishTag) || !fishTag.contains("fish")) return;
            Holder<Item> item = BuiltInRegistries.ITEM.wrapAsHolder(BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(fishTag.getString("fish"))));
            fishData.put(item, MaidFishData.readFrom(fishTag, "data").orElse(new MaidFishData()));
        });
    }

    /**
     * 获取或创建玩家的指定女仆的钓鱼数据
     */
    public static MaidFishingData getOrCreate(Player player, UUID maidUuid) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getOrCreate(serverPlayer, maidUuid);
        }
        return CLIENT_DATA.computeIfAbsent(maidUuid, MaidFishingData::new);
    }

    private static MaidFishingData getOrCreate(ServerPlayer player, UUID maidUuid) {
        String cacheKey = player.getUUID() + "_" + maidUuid;
        
        // 从缓存获取
        if (SERVER_CACHE.containsKey(cacheKey)) {
            return SERVER_CACHE.get(cacheKey);
        }

        // 从NBT加载
        CompoundTag playerPersistentData = player.getPersistentData();
        MaidFishingData data;
        
        if (playerPersistentData.contains(NBT_TAG)) {
            CompoundTag maidDataTag = playerPersistentData.getCompound(NBT_TAG);
            String maidKey = maidUuid.toString();
            if (maidDataTag.contains(maidKey)) {
                data = new MaidFishingData(maidUuid, maidDataTag.getCompound(maidKey));
            } else {
                data = new MaidFishingData(maidUuid);
            }
        } else {
            data = new MaidFishingData(maidUuid);
        }
        
        SERVER_CACHE.put(cacheKey, data);
        return data;
    }

    /**
     * 将当前数据保存到玩家的持久数据中
     */
    public void saveToPlayer(ServerPlayer player) {
        CompoundTag playerPersistentData = player.getPersistentData();
        CompoundTag maidDataTag = playerPersistentData.getCompound(NBT_TAG);
        maidDataTag.put(maidUuid.toString(), getAsTag());
        playerPersistentData.put(NBT_TAG, maidDataTag);
    }

    /**
     * 将数据保存为 NBT
     */
    public CompoundTag getAsTag() {
        CompoundTag tag = new CompoundTag();

        ListTag fishList = new ListTag();
        fishData.forEach((item, data) -> {
            CompoundTag fishTag = new CompoundTag();
            fishTag.putString("fish", BuiltInRegistries.ITEM.getKey(item.value()).toString());
            data.saveTo(fishTag, "data");
            fishList.add(fishTag);
        });

        tag.put("fish_data", fishList);
        return tag;
    }

    /**
     * 同步数据到指定玩家（客户端）并保存到服务端NBT
     */
    public void syncTo(ServerPlayer player) {
        CompoundTag tag = getAsTag();

        saveToPlayer(player);

        SyncMaidFishingDataMsg msg = new SyncMaidFishingDataMsg(maidUuid, tag);
        player.connection.send(new ClientboundCustomPayloadPacket(msg));
    }

    /**
     * 检查鱼是否已解锁
     */
    public boolean isFishUnlocked(Holder<Item> holder) {
        return Optional.ofNullable(fishData.get(holder))
                .map(data -> data.isUnlocked)
                .orElse(false);
    }

    /**
     * 检查鱼是否已解锁（通过 ItemStack）
     */
    public boolean isFishUnlocked(ItemStack stack) {
        return fishData.entrySet().stream()
                .filter(entry -> stack.is(entry.getKey()))
                .anyMatch(entry -> entry.getValue().isUnlocked);
    }

    /**
     * 解锁鱼
     */
    public void unlockFish(Holder<Item> fish) {
        if (!fishData.containsKey(fish)) {
            fishData.put(fish, new MaidFishData(true, true, false, Optional.empty()));
            return;
        }

        MaidFishData data = fishData.get(fish);
        if (data.isUnlocked) return;

        data.isUnlocked = true;
        data.isUnread = true;
    }

    /**
     * 标记为已读
     */
    public void markAsRead(ItemStack stack) {
        Holder<Item> fish = stack.getItemHolder();
        if (!fishData.containsKey(fish)) return;
        fishData.get(fish).isUnread = false;
    }

    /**
     * 检查是否未读
     */
    public boolean isUnread(ItemStack stack) {
        Holder<Item> fish = stack.getItemHolder();
        if (!fishData.containsKey(fish)) return false;
        return fishData.get(fish).isUnread;
    }

    /**
     * 检查是否有笔记
     */
    public boolean hasNote(ItemStack stack) {
        Holder<Item> fish = stack.getItemHolder();
        if (!fishData.containsKey(fish)) return false;
        return fishData.get(fish).hasNote;
    }

    /**
     * 记录捕获
     */
    public void logCatch(ItemStack stack, @Nullable Level level) {
        FishData data = FishData.get(stack).orElse(null);
        if (data == null) return;

        Holder<Item> fish = data.fish();

        if (!fishData.containsKey(fish)) {
            fishData.put(fish, new MaidFishData(true, true, false, Optional.empty()));
        } else {
            MaidFishData existing = fishData.get(fish);
            if (!existing.isUnlocked) {
                existing.isUnlocked = true;
                existing.isUnread = true;
            }
        }

        MaidFishData maidData = fishData.get(fish);
        FishStats stats = maidData.stats.orElse(new FishStats());

        if (data.size().isPresent() && level != null) {
            double fishLength = 0;
            if (TideItemData.FISH_LENGTH.isPresent(stack))
                fishLength = TideItemData.FISH_LENGTH.getOrDefault(stack, 0.0);
            else
                fishLength = data.getRandomLength(level.random);
            stats.logCatch(CatchTimestamp.now(level), fishLength);
        }

        fishData.put(fish, new MaidFishData(maidData.isUnlocked, maidData.isUnread, maidData.hasNote, Optional.of(stats)));
    }

    /**
     * 标记笔记已解锁
     */
    public void markNoteUnlocked(Item item) {
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        MaidFishData maidData = fishData.get(holder);

        if (maidData == null) {
            maidData = new MaidFishData();
            maidData.hasNote = true;
            maidData.isUnread = true;
            fishData.put(holder, maidData);
        } else {
            maidData.hasNote = true;
            maidData.isUnread = true;
        }
    }

    public UUID getMaidUuid() {
        return maidUuid;
    }

    /**
     * 获取指定鱼的统计信息
     */
    public Optional<FishStats> getFishStats(ItemStack stack) {
        Holder<Item> fish = stack.getItemHolder();
        return Optional.ofNullable(fishData.get(fish))
                .flatMap(maidData -> maidData.stats);
    }

    /**
     * 单条鱼的钓鱼数据（独立于 Tide 的 FishPlayerData）
     */
    public static class MaidFishData {
        public static final Codec<MaidFishData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("is_unlocked").forGetter(data -> data.isUnlocked),
                Codec.BOOL.fieldOf("is_unread").forGetter(data -> data.isUnread),
                Codec.BOOL.optionalFieldOf("has_note", false).forGetter(data -> data.hasNote),
                FishStats.CODEC.optionalFieldOf("stats").forGetter(data -> data.stats)
        ).apply(instance, MaidFishData::new));

        public boolean isUnlocked;
        public boolean isUnread;
        public boolean hasNote;
        public Optional<FishStats> stats;

        public MaidFishData() {
            this(false, false, false, Optional.empty());
        }

        public MaidFishData(FishStats stats) {
            this(false, false, false, Optional.of(stats));
        }

        public MaidFishData(boolean isUnlocked, boolean isUnread, boolean hasNote, Optional<FishStats> stats) {
            this.isUnlocked = isUnlocked;
            this.isUnread = isUnread;
            this.hasNote = hasNote;
            this.stats = stats;
        }

        public void saveTo(CompoundTag tag, String key) {
            tag.put(key, CODEC.encode(this, NbtOps.INSTANCE, new CompoundTag()).result().orElseThrow());
        }

        public static Optional<MaidFishData> readFrom(CompoundTag tag, String key) {
            try {
                return CODEC.decode(NbtOps.INSTANCE, tag.get(key)).result().map(Pair::getFirst);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }
}
