package com.chadate.tidecompat.debug;

import com.chadate.tidecompat.TideTouhoulittlemaidCompat;
import com.chadate.tidecompat.network.DebugFishingSyncPayload;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 注册 /tidecompat debug_fishing 命令
 */
public class DebugFishingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tidecompat")
                .then(Commands.literal("debug_fishing")
                        .executes(ctx -> toggle(ctx.getSource(), null))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> toggle(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))
                        )
                )
        );
    }

    private static int toggle(CommandSourceStack source, Boolean enabled) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令仅玩家可执行"));
            return 0;
        }

        if (enabled == null) {
            DebugFishingState.toggle(player.getUUID());
        } else if (enabled) {
            DebugFishingState.getEnabled().add(player.getUUID());
        } else {
            DebugFishingState.getEnabled().remove(player.getUUID());
        }

        boolean isOn = DebugFishingState.isEnabled(player.getUUID());
        source.sendSuccess(() -> Component.literal(
                "潮汐钓鱼 DEBUG 渲染已" + (isOn ? "开启" : "关闭")
        ), false);

        DebugFishingSyncPayload payload = new DebugFishingSyncPayload(isOn);
        player.connection.send(payload);

        return Command.SINGLE_SUCCESS;
    }
}
