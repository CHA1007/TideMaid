package com.chadate.tidecompat.network;

import com.chadate.tidecompat.TideTouhoulittlemaidCompat;
import com.chadate.tidecompat.client.render.DebugFishingRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务器 → 客户端：同步 DEBUG 渲染开关状态
 */
public record DebugFishingSyncPayload(boolean enabled) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TideTouhoulittlemaidCompat.MOD_ID, "debug_fishing_sync");
    public static final Type<DebugFishingSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugFishingSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            DebugFishingSyncPayload::enabled,
            DebugFishingSyncPayload::new
    );

    public static void handle(DebugFishingSyncPayload message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> DebugFishingRenderer.setEnabled(message.enabled()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
