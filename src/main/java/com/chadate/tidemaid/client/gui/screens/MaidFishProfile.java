package com.chadate.tidemaid.client.gui.screens;

import com.chadate.tidemaid.client.gui.components.MaidStatsComponent;
import com.chadate.tidemaid.data.MaidFishingData;
import com.li64.tide.Tide;
import com.li64.tide.client.gui.screens.journal.FishingJournal;
import com.li64.tide.client.gui.screens.journal.ProfileComponent;
import com.li64.tide.client.gui.screens.journal.components.*;
import com.li64.tide.compat.seasons.SeasonsCompat;
import com.li64.tide.data.fishing.FishData;
import com.li64.tide.data.fishing.conditions.types.*;
import com.li64.tide.data.fishing.modifiers.types.TemperatureModifier;
import com.li64.tide.data.journal.FishRarity;
import com.li64.tide.data.player.FishStats;
import com.li64.tide.util.TideUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 女仆百鱼全书的鱼类详情面板
 */
public class MaidFishProfile implements Renderable {
    private static final ResourceLocation BORDERS = Tide.resource("textures/gui/journal/profile_borders.png");
    private static final ResourceLocation STAR = Tide.resource("textures/gui/journal/star.png");
    private static final ResourceLocation LINE_BOTTOM = Tide.resource("journal/line_bottom");

    private static final int BG_WIDTH = 400;
    private static final int BG_HEIGHT = 260;
    private static final int TEXT_COLOR = FishingJournal.TEXT_COLOR;

    private final ItemStack fish;
    private final FishData data;
    private final List<ProfileComponent> profileComponents;
    private final Font font;
    private final Component description;
    private final FishRarity rarity;
    private final Component rarityPrefix;
    private final UUID maidUuid;
    private final MaidFishingData maidData;

    public MaidFishProfile(ItemStack fish, Font font, UUID maidUuid, MaidFishingData maidData) {
        this.font = font;
        this.fish = fish;
        this.maidUuid = maidUuid;
        this.maidData = maidData;
        this.data = FishData.get(fish).orElse(null);

        if (data != null) {
            this.rarity = data.profile().rarity();
            this.description = buildDescription();
            this.rarityPrefix = buildRarityPrefix();
            this.profileComponents = buildComponentsWithStats(fish, data, maidData);
        } else {
            this.rarity = FishRarity.COMMON;
            this.description = Component.translatable("maid_journal.description.missing");
            this.rarityPrefix = Component.translatable("maid_journal.rarity.common");
            this.profileComponents = new ArrayList<>();
        }
    }

    private Component buildDescription() {
        String descKey = data.profile().description().orElse("maid_journal.description.missing");
        Component desc = Component.translatable(descKey);
        String str = desc.getString();
        if (str.contains("journal.description") || str.isBlank()) {
            desc = Component.translatable("maid_journal.description.missing");
        }
        return desc;
    }

    private Component buildRarityPrefix() {
        Style defaultStyle = Component.empty().getStyle();
        return Component.translatable("maid_journal.rarity.title")
                .append(": ")
                .append(Component.translatable("maid_journal.rarity." + rarity.getKey()).withStyle(defaultStyle.withColor(rarity.getColor())))
                .append(" (");
    }

    public static ArrayList<ProfileComponent> buildComponentsWithStats(ItemStack fish, FishData data, MaidFishingData maidData) {
        ArrayList<ProfileComponent> builder = buildComponents(data);

        if (!builder.isEmpty() && !(builder.getLast() instanceof HorizontalLineComponent)) {
            builder.add(new HorizontalLineComponent(false));
        }
        FishStats stats = maidData != null ? maidData.getFishStats(fish).orElse(new FishStats()) : new FishStats();
        builder.add(new MaidStatsComponent(stats));

        return builder;
    }

    public static ArrayList<ProfileComponent> buildComponents(FishData data) {
        ArrayList<ProfileComponent> builder = new ArrayList<>(5);

        data.profile().location().ifPresent(location ->
                builder.add(new LocationComponent(Component.translatable(location))));

        Optional<LuckCondition> luckCondition = data.conditions().stream()
                .filter(cond -> cond instanceof LuckCondition)
                .findFirst().map(cond -> (LuckCondition) cond);
        luckCondition.ifPresent(condition ->
                builder.add(new LuckComponent(condition.getMinLuck(), condition.getMaxLuck())));

        if (!builder.isEmpty()) {
            builder.add(new HorizontalLineComponent(false));
        }

        Optional<DimensionsCondition> dimensionsCondition = data.conditions().stream()
                .filter(cond -> cond instanceof DimensionsCondition)
                .findFirst().map(cond -> (DimensionsCondition) cond);
        dimensionsCondition.ifPresent(condition -> {
            if (DimensionsComponent.shouldCreate(condition)) {
                builder.add(new DimensionsComponent(condition.getDimensions()));
            }
        });

        if (SeasonsCompat.isActive()) {
            Optional<SeasonsCondition> seasonsCondition = data.conditions().stream()
                    .filter(cond -> cond instanceof SeasonsCondition)
                    .findFirst().map(cond -> (SeasonsCondition) cond);
            seasonsCondition.ifPresent(condition ->
                    builder.add(new SeasonsComponent(condition.getSeasons())));
        }

        Optional<TemperatureModifier> tempMod = data.modifiers().stream()
                .filter(modx -> modx instanceof TemperatureModifier)
                .findFirst().map(modx -> (TemperatureModifier) modx);
        if (tempMod.isPresent()) {
            TemperatureModifier mod = tempMod.get();
            float minTemp = Math.max(mod.getPreferred() - mod.getTolerance() * 0.95F, -1.0F);
            float maxTemp = Math.max(mod.getPreferred() + mod.getTolerance() * 0.95F, -1.0F);
            builder.add(new TemperatureComponent(minTemp, maxTemp));
        }

        Optional<TimeOfDayCondition> timeCondition = data.conditions().stream()
                .filter(cond -> cond instanceof TimeOfDayCondition)
                .findFirst().map(cond -> (TimeOfDayCondition) cond);
        timeCondition.ifPresent(condition ->
                builder.add(new TimeComponent(condition.getRanges())));

        Optional<DepthRangeHolder> depthRange = data.conditions().stream()
                .filter(cond -> cond instanceof DepthRangeHolder)
                .findFirst().map(cond -> (DepthRangeHolder) cond);
        if (depthRange.isPresent()) {
            Optional<DimensionsCondition> dimensionCondition = data.conditions().stream()
                    .filter(cond -> cond instanceof DimensionsCondition)
                    .findFirst().map(cond -> (DimensionsCondition) cond);
            if (dimensionCondition.map(DimensionsCondition::isOverworldOnly).orElse(false)) {
                DepthRangeHolder range = depthRange.get();
                float min = range.hasLowerBound() ? DepthComponent.depthToFloat(range.getMinY()) : 0.0F;
                float max = range.hasUpperBound() ? DepthComponent.depthToFloat(range.getMaxY()) : 1.0F;
                if (min < 0.7F || max != 1.0F) {
                    builder.add(new DepthComponent(min, max));
                }
            }
        }

        Optional<MoonPhaseCondition> moonPhaseCondition = data.conditions().stream()
                .filter(cond -> cond instanceof MoonPhaseCondition)
                .findFirst().map(cond -> (MoonPhaseCondition) cond);
        moonPhaseCondition.ifPresent(condition ->
                builder.add(new MoonPhaseComponent(condition.getPhases())));

        Optional<WeatherCondition> weatherCondition = data.conditions().stream()
                .filter(cond -> cond instanceof WeatherCondition)
                .findFirst().map(cond -> (WeatherCondition) cond);
        weatherCondition.ifPresent(condition ->
                builder.add(new WeatherComponent(condition.getWeatherTypes())));

        return builder;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int tlX = (graphics.guiWidth() - BG_WIDTH) / 2;
        int tlY = (graphics.guiHeight() - BG_HEIGHT) / 2;
        graphics.blit(BORDERS, tlX, tlY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        int titleX = tlX + 110;
        int titleY = tlY + 30;
        Component title = TideUtils.removeRawTextInName(fish.getHoverName());
        int titleWidth = font.width(title);
        int underlineWidth = titleWidth + 6;
        graphics.drawString(font, title, titleX - titleWidth / 2, titleY, TEXT_COLOR, false);
        graphics.blitSprite(LINE_BOTTOM, titleX - underlineWidth / 2,
                titleY + font.lineHeight + 2, underlineWidth, 2);

        boolean isLarge = data.profile().altSprite().isPresent();
        ResourceLocation alternateTexture = data.profile().altSprite().orElse(null);
        int fishSize = isLarge ? data.profile().altSpriteSize().orElse(16) : 16;
        int itemCenterX = tlX + 109;
        int itemCenterY = tlY + 100;
        final int shadowOffset = 2;
        final float scale = 2f;

        graphics.pose().pushPose();
        graphics.pose().translate(itemCenterX, itemCenterY, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.pose().translate(-itemCenterX, -itemCenterY, 0);

        graphics.pose().pushPose();
        graphics.pose().translate(shadowOffset / scale, shadowOffset / scale, 0f);

        graphics.flush();
        RenderSystem.setShaderColor(0.8431f, 0.7098f, 0.5804f, 1f);

        if (!isLarge) FishingJournal.renderItemSilhouette(graphics, fish,
                itemCenterX - fishSize / 2, itemCenterY - fishSize / 2);
        else FishingJournal.renderTextureSilhouette(graphics, alternateTexture,
                itemCenterX - fishSize / 2, itemCenterY - fishSize / 2, fishSize, fishSize);

        graphics.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        graphics.pose().popPose();

        if (!isLarge) graphics.renderItem(fish, itemCenterX - fishSize / 2, itemCenterY - fishSize / 2);
        else graphics.blit(alternateTexture,
                itemCenterX - fishSize / 2, itemCenterY - fishSize / 2,
                0, 0, fishSize, fishSize, fishSize, fishSize);

        graphics.pose().popPose();

        int stars = rarity.getNumStars();
        int rarityX = tlX + 40;
        int rarityY = tlY + 167;
        graphics.drawString(font, rarityPrefix, rarityX, rarityY, TEXT_COLOR, false);
        for (int i = 0; i < stars; i++) graphics.blit(STAR,
                rarityX + font.width(rarityPrefix) + i * 8, rarityY, 0, 0,
                7, 6, 7, 6);
        graphics.drawString(font, ")", rarityX + font.width(rarityPrefix) + stars * 8, rarityY, TEXT_COLOR, false);

        int descX = tlX + 38;
        int descY = tlY + 207;
        List<FormattedCharSequence> descriptionLines = font.split(this.description, 156);
        descY -= (descriptionLines.size() * font.lineHeight) / 2;
        for (int i = 0; i < descriptionLines.size(); i++) {
            graphics.drawString(font, descriptionLines.get(i), descX,
                    descY + i * font.lineHeight, TEXT_COLOR, false);
        }

        int padding = 4;
        int cursorY = padding + 8;
        for (ProfileComponent area : profileComponents) {
            area.render(graphics, font, tlX + ProfileComponent.AREA_X,
                    tlY + ProfileComponent.AREA_Y + cursorY, mouseX, mouseY, partialTick);
            cursorY += area.getRequiredHeight() + padding;
        }
    }

    @SuppressWarnings("unused")
    public UUID getMaidUuid() {
        return maidUuid;
    }
}
