package com.chadate.tidemaid.entity.chatbubble;

import com.chadate.tidemaid.TideMaid;
import com.chadate.tidemaid.client.renderer.chatbubble.ItemChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.IChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemChatBubbleData implements IChatBubbleData {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TideMaid.MOD_ID, "item");

    private final int existTick;
    private final int priority;
    private final ItemStack itemStack;

    @OnlyIn(Dist.CLIENT)
    private IChatBubbleRenderer renderer;

    private ItemChatBubbleData(int existTick, int priority, ItemStack itemStack) {
        this.existTick = existTick;
        this.priority = priority;
        this.itemStack = itemStack;
    }

    public static ItemChatBubbleData create(int existTick, int priority, ItemStack itemStack) {
        return new ItemChatBubbleData(existTick, priority, itemStack);
    }

    @Override
    public int existTick() {
        return this.existTick;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IChatBubbleRenderer getRenderer(IChatBubbleRenderer.Position position) {
        if (this.renderer == null) {
            this.renderer = new ItemChatBubbleRenderer(this.itemStack);
        }
        return this.renderer;
    }

    public static class ItemChatSerializer implements IChatBubbleData.ChatSerializer {
        @Override
        public IChatBubbleData readFromBuff(FriendlyByteBuf buf) {
            RegistryFriendlyByteBuf regBuf = (RegistryFriendlyByteBuf) buf;
            ItemStack itemStack = ItemStack.STREAM_CODEC.decode(regBuf);
            return new ItemChatBubbleData(DEFAULT_EXIST_TICK, DEFAULT_PRIORITY, itemStack);
        }

        @Override
        public void writeToBuff(FriendlyByteBuf buf, IChatBubbleData data) {
            RegistryFriendlyByteBuf regBuf = (RegistryFriendlyByteBuf) buf;
            ItemChatBubbleData itemChat = (ItemChatBubbleData) data;
            ItemStack.STREAM_CODEC.encode(regBuf, itemChat.itemStack);
        }
    }
}