package com.chadate.tidecompat.debug;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEBUG 渲染状态管理
 * 记录哪些玩家开启了潮汐钓鱼搜索范围可视化
 */
public class DebugFishingState {
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(UUID player) {
        return ENABLED_PLAYERS.contains(player);
    }

    public static void toggle(UUID player) {
        if (ENABLED_PLAYERS.contains(player)) {
            ENABLED_PLAYERS.remove(player);
        } else {
            ENABLED_PLAYERS.add(player);
        }
    }

    public static Set<UUID> getEnabled() {
        return ENABLED_PLAYERS;
    }
}
