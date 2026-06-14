package com.chadate.tidemaid.network;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.data.MaidFishingData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 客户端请求同步女仆钓鱼数据的消息（客户端→服务端）
 */
public record RequestMaidFishingDataMsg(UUID maidUuid) implements CustomPacketPayload {
    public static final Type<RequestMaidFishingDataMsg> TYPE = new Type<>(TideMaid.resource("request_maid_fishing_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMaidFishingDataMsg> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull RequestMaidFishingDataMsg decode(RegistryFriendlyByteBuf buf) {
            return new RequestMaidFishingDataMsg(buf.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, RequestMaidFishingDataMsg msg) {
            buf.writeUUID(msg.maidUuid);
        }
    };

    public static void handle(RequestMaidFishingDataMsg message, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        MaidFishingData data = MaidFishingData.getOrCreate(serverPlayer, message.maidUuid);
        data.syncTo(serverPlayer);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
