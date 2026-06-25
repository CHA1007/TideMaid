package com.chadate.tidemaid.client.gui.components;

import com.chadate.tidemaid.client.gui.screens.MaidFishingJournal;
import com.google.common.collect.ImmutableList;
import com.li64.tide.Tide;
import com.li64.tide.client.gui.screens.journal.ProfileComponent;
import com.li64.tide.data.player.CatchTimestamp;
import com.li64.tide.data.player.FishStats;
import com.li64.tide.util.TideUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * 女仆百鱼全书专用的统计信息组件
 */
public class MaidStatsComponent extends ProfileComponent {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

    private final List<Component> lines;

    public MaidStatsComponent(FishStats stats) {
        ImmutableList.Builder<Component> builder = ImmutableList.builder();
        builder.add(Component.translatable("maid_journal.info.stats.total", stats.getAmountCaught()));

        if (!stats.isEmpty()) {
            if (stats.getInitialCatchDate().isPresent()) {
                CatchTimestamp timestamp = stats.getInitialCatchDate().get();
                Component formatted;

                if (Tide.CLIENT_CONFIG.journal.useRealDate) {
                    Instant instant = timestamp.date();
                    ZonedDateTime localTime = instant.atZone(ZoneId.systemDefault());
                    formatted = Component.literal(localTime.format(DATE_FORMAT));
                }
                else formatted = Component.translatable("maid_journal.info.stats.day", (int)(timestamp.ticks() / 24000L));

                builder.add(Component.translatable("maid_journal.info.stats.first", formatted));
            }

            if (stats.getLargestCatch() > 0.0) {
                builder.add(Component.translatable("maid_journal.info.stats.largest",
                        TideUtils.getFormattedLength(stats.getLargestCatch())));
                builder.add(Component.translatable("maid_journal.info.stats.smallest",
                        TideUtils.getFormattedLength(stats.getSmallestCatch())));
            }
        }
        this.lines = builder.build();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY, float partialTick) {
        int center = x + AREA_WIDTH / 2;
        int cursorY = 0;
        for (Component line : lines) {
            graphics.drawString(font, line, center - font.width(line) / 2, y + cursorY, MaidFishingJournal.TEXT_COLOR, false);
            cursorY += 11;
        }
    }

    @Override
    public int getRequiredHeight() {
        return lines.size() * 11;
    }
}
