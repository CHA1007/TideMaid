package com.chadate.tidemaid.network;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.data.MaidFishingData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 同步女仆钓鱼数据到客户端的网络消息
 */
public record SyncMaidFishingDataMsg(UUID maidUuid, CompoundTag tag) implements CustomPacketPayload {
    public static final Type<SyncMaidFishingDataMsg> TYPE = new Type<>(TideMaid.resource("sync_maid_fishing_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMaidFishingDataMsg> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SyncMaidFishingDataMsg decode(RegistryFriendlyByteBuf buf) {
            return new SyncMaidFishingDataMsg(buf.readUUID(), buf.readNbt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncMaidFishingDataMsg msg) {
            buf.writeUUID(msg.maidUuid);
            buf.writeNbt(msg.tag);
        }
    };

    public static void handle(SyncMaidFishingDataMsg message, Player player) {
        MaidFishingData data = new MaidFishingData(message.maidUuid, message.tag);
        MaidFishingData.CLIENT_DATA.put(message.maidUuid, data);

        // 通知客户端刷新
        MaidFishingData.notifyDataSync(message.maidUuid);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
