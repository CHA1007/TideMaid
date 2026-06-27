package com.chadate.tidemaid.network;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.data.MaidFishingData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 标记女仆钓鱼条目已读的网络消息（客户端→服务端）
 */
public record ReadMaidProfileMsg(UUID maidUuid, int fishId) implements CustomPacketPayload {
    public static final Type<ReadMaidProfileMsg> TYPE = new Type<>(TideMaid.resource("read_maid_profile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReadMaidProfileMsg> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ReadMaidProfileMsg decode(RegistryFriendlyByteBuf buf) {
            return new ReadMaidProfileMsg(buf.readUUID(), buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ReadMaidProfileMsg msg) {
            buf.writeUUID(msg.maidUuid);
            buf.writeInt(msg.fishId);
        }
    };

    public ReadMaidProfileMsg(UUID maidUuid, ItemStack fishItem) {
        this(maidUuid, Item.getId(fishItem.getItem()));
    }

    public static void handle(ReadMaidProfileMsg message, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // 验证女仆是否存在且属于该玩家
        Entity entity = ((ServerLevel) serverPlayer.level()).getEntity(message.maidUuid);
        if (!(entity instanceof EntityMaid maid)) return;
        if (!maid.isOwnedBy(serverPlayer)) return;
        
        MaidFishingData data = MaidFishingData.getOrCreate(serverPlayer, message.maidUuid);
        Item fish = Item.byId(message.fishId);
        data.markAsRead(new ItemStack(fish));
        data.syncTo(serverPlayer);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
