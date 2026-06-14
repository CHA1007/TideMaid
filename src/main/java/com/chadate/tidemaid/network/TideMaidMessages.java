package com.chadate.tidemaid.network;

import com.chadate.tidemaid.TideMaid;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络消息注册
 */
public class TideMaidMessages {

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TideMaid.MOD_ID).versioned("1.0.0");

        // 客户端接收：同步女仆钓鱼数据
        registrar.playToClient(
                SyncMaidFishingDataMsg.TYPE,
                SyncMaidFishingDataMsg.CODEC,
                TideMaidMessages::handleSyncMaidFishingData
        );

        // 服务端接收：请求同步女仆钓鱼数据
        registrar.playToServer(
                RequestMaidFishingDataMsg.TYPE,
                RequestMaidFishingDataMsg.CODEC,
                TideMaidMessages::handleRequestMaidFishingData
        );

        // 服务端接收：标记女仆钓鱼条目已读
        registrar.playToServer(
                ReadMaidProfileMsg.TYPE,
                ReadMaidProfileMsg.CODEC,
                TideMaidMessages::handleReadMaidProfile
        );
    }

    private static void handleSyncMaidFishingData(SyncMaidFishingDataMsg msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SyncMaidFishingDataMsg.handle(msg, ctx.player()));
    }

    private static void handleRequestMaidFishingData(RequestMaidFishingDataMsg msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> RequestMaidFishingDataMsg.handle(msg, ctx.player()));
    }

    private static void handleReadMaidProfile(ReadMaidProfileMsg msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ReadMaidProfileMsg.handle(msg, ctx.player()));
    }
}
