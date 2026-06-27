package com.chadate.tidemaid.client.gui.screens;

import com.chadate.tidemaid.data.MaidFishingData;
import com.chadate.tidemaid.network.ReadMaidProfileMsg;
import com.chadate.tidemaid.network.RequestMaidFishingDataMsg;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.li64.tide.Tide;
import com.li64.tide.client.gui.screens.FishyNoteScreen;
import com.li64.tide.client.gui.screens.journal.FishingJournal;
import com.li64.tide.data.TideData;
import com.li64.tide.data.fishing.FishData;
import com.li64.tide.data.journal.JournalGroup;
import com.li64.tide.registries.TideSoundEvents;
import com.li64.tide.util.TideUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 女仆百鱼全书界面
 */
public class MaidFishingJournal extends Screen {
    public static final int TEXT_COLOR = FishingJournal.TEXT_COLOR;

    private static final ResourceLocation BACKGROUND = Tide.resource("textures/gui/journal/journal_bg.png");
    private static final int BG_WIDTH = 400;
    private static final int BG_HEIGHT = 260;

    private static final ResourceLocation LINE_BOTTOM = TideUtils.sprite("journal/line_bottom");

    private static final ResourceLocation FISHY_NOTE_ICON = Tide.resource("textures/gui/journal/fishy_note_icon.png");
    private static final ResourceLocation FISHY_NOTE_ICON_SELECTED = Tide.resource("textures/gui/journal/fishy_note_icon_selected.png");

    private static final int CELL_SIZE = 22;
    private static final int GROUP_SPACING = 10;
    private static final int FISH_PER_ROW = 14;

    private final Player player;
    private final EntityMaid maid;
    private MaidFishingData maidFishingData;
    private final List<Map<JournalGroup, List<Holder<Item>>>> fishByPage;
    private int page = 0;

    private @Nullable ItemStack activeFish = null;
    private @Nullable FishyNoteScreen activeNote = null;
    private @Nullable MaidFishProfile profile = null;
    private @Nullable Button xButton = null;
    private @Nullable Button leftButton = null;
    private @Nullable Button rightButton = null;
    private float yOffset = 3f;
    private boolean didClick;

    public MaidFishingJournal(Player player, EntityMaid maid) {
        super(GameNarrator.NO_TITLE);
        this.player = player;
        this.maid = maid;
        this.maidFishingData = MaidFishingData.getOrCreate(player, maid.getUUID());

        // 向服务端请求同步女仆钓鱼数据
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(
                new ServerboundCustomPayloadPacket(new RequestMaidFishingDataMsg(maid.getUUID())));

        this.fishByPage = paginate(TideData.FISH.get().valueStream()
                .filter(FishData::hasJournalEntry)
                .sorted(Comparator.comparing((FishData a) -> a.profile().group().ordinal())
                        .thenComparing(a -> a.profile().rarity().ordinal())
                        .thenComparingDouble(d -> -d.weight()))
                .collect(Collectors.groupingBy(
                        data -> data.profile().group(),
                        LinkedHashMap::new,
                        Collectors.mapping(FishData::fish, Collectors.toList())
                )));

        this.init();
        player.playSound(TideSoundEvents.JOURNAL_OPEN, 0.9f, 1.0f + new Random().nextFloat() * 0.2f);
    }

    public static List<Map<JournalGroup, List<Holder<Item>>>> paginate(Map<JournalGroup, List<Holder<Item>>> groupedFish) {
        return FishingJournal.paginate(groupedFish);
    }

    /**
     * 收到服务端同步数据后刷新界面
     */
    public void onDataSync(UUID syncMaidUuid) {
        if (!syncMaidUuid.equals(maid.getUUID())) return;
        this.maidFishingData = MaidFishingData.CLIENT_DATA.get(maid.getUUID());
        if (activeFish != null && maidFishingData != null) {
            this.profile = new MaidFishProfile(activeFish, this.font, maid.getUUID(), maidFishingData);
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("gui.tidemaid.screen.maid_fishing_journal");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void pageChanged() {
        yOffset = 3f;
        player.playSound(TideSoundEvents.PAGE_FLIP, 1.0f, 1.0f + new Random().nextFloat() * 0.2f);
        this.init();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.leftButton = null;
        this.rightButton = null;
        int xX = Math.min(this.width - 19, (width + BG_WIDTH) / 2 - 27);
        int xY = Math.max(3, (height - BG_HEIGHT) / 2 + 12);
        int leftX = Math.max(3, (width - BG_WIDTH) / 2 + 11);
        int rightX = Math.min(this.width - 19, (width + BG_WIDTH) / 2 - 27);
        if (activeFish != null) {
            this.activeNote = null;
            this.profile = new MaidFishProfile(activeFish, MaidFishingJournal.this.font, maid.getUUID(), maidFishingData);
            this.xButton = Button.builder(Component.literal("X"), button -> {
                this.activeFish = null;
                this.pageChanged();
            }).bounds(xX, xY, 16, 16).build();
        }
        else if (activeNote != null) {
            this.profile = null;
            xX = Math.min(width - 19, (width + 200) / 2 - 16);
            xY = Math.max(3, (height - 230) / 2);
            this.xButton = Button.builder(Component.literal("X"), button -> {
                this.activeNote = null;
                this.pageChanged();
            }).bounds(xX, xY, 16, 16).build();
        }
        else {
            this.profile = null;
            this.xButton = Button.builder(Component.literal("X"), button -> onClose())
                    .bounds(xX, xY, 16, 16).build();
            if (page > 0) {
                this.leftButton = Button.builder(Component.literal("<<"), button -> pageLeft())
                        .bounds(leftX, this.height / 2 - 8, 16, 16).build();
                this.addWidget(leftButton);
            }
            if (page < fishByPage.size() - 1) {
                this.rightButton = Button.builder(Component.literal(">>"), button -> pageRight())
                        .bounds(rightX, this.height / 2 - 8, 16, 16).build();
                this.addWidget(rightButton);
            }
        }
        this.addWidget(xButton);
    }

    private void pageLeft() {
        page--;
        if (page < 0) page = fishByPage.size() - 1;
        this.pageChanged();
    }

    private void pageRight() {
        page++;
        if (page == fishByPage.size()) page = 0;
        this.pageChanged();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) this.didClick = true;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return switch (keyCode) {
            case 256, 69 -> {
                if (this.activeFish != null) {
                    this.activeFish = null;
                    this.pageChanged();
                }
                else if (this.activeNote != null) {
                    this.activeNote = null;
                    this.pageChanged();
                }
                else this.onClose();
                yield true;
            }
            case 262 -> {
                if (this.activeFish == null) pageRight();
                yield true;
            }
            case 263 -> {
                if (this.activeFish == null) pageLeft();
                yield true;
            }
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        player.playSound(TideSoundEvents.JOURNAL_CLOSE, 0.9f, 1.0f + new Random().nextFloat() * 0.2f);
    }

    @Override
    public void render(@org.jetbrains.annotations.NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float deltaTicks = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        super.render(graphics, mouseX, mouseY, partialTick);

        boolean updatePage = false;

        this.yOffset = Math.max(yOffset - yOffset * 0.65f * deltaTicks, 0f);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, -(int) yOffset, 0f);

        int tlX = (width - BG_WIDTH) / 2;
        int tlY = (height - BG_HEIGHT) / 2;
        if (activeNote == null) graphics.blit(BACKGROUND, tlX, tlY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        if (profile != null) profile.render(graphics, mouseX, mouseY, partialTick);
        else if (activeNote != null) activeNote.render(graphics, mouseX, mouseY, partialTick);
        else {
            final int hoverTolerance = 3;
            int cursorY = 0;

            int groupIndex = 0;
            for (var fish : fishByPage.get(page).values()) {
                int gridX = tlX + 44;
                int gridY = tlY + 39 + cursorY;
                int i = 0;

                for (Holder<Item> fishItem : fish) {
                    int cellX = i % FISH_PER_ROW;
                    int cellY = Mth.floor((float) i / FISH_PER_ROW);
                    int x = gridX + cellX * CELL_SIZE;
                    int y = gridY + cellY * CELL_SIZE;
                    ItemStack stack = new ItemStack(fishItem);
                    // 使用女仆的钓鱼数据而非玩家数据
                    boolean isUnlocked = maidFishingData.isFishUnlocked(fishItem);
                    boolean isUnread = maidFishingData.isUnread(stack);
                    boolean hasNote = maidFishingData.hasNote(stack);
                    boolean isHovering = false;

                    if (mouseX > x - hoverTolerance && mouseY > y - hoverTolerance
                            && mouseX < x + 16 + hoverTolerance && mouseY < y + 16 + hoverTolerance) {
                        isHovering = true;
                        if (isUnread) {
                            maidFishingData.markAsRead(stack);
                            Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(new ServerboundCustomPayloadPacket(new ReadMaidProfileMsg(maid.getUUID(), stack)));
                        }
                        if (isUnlocked) {
                            graphics.renderTooltip(this.font, stack.getHoverName(), mouseX, mouseY);
                            if (didClick && !updatePage) {
                                activeFish = stack;
                                updatePage = true;
                            }
                        }
                        else {
                            FishData.get(stack);
                            if (hasNote && didClick && !updatePage) {
                                activeNote = new FishyNoteScreen(stack);
                                updatePage = true;
                            }
                        }
                    }

                    graphics.flush();
                    RenderSystem.setShaderColor(0.8431f, 0.7098f, 0.5804f, 1f);
                    FishingJournal.renderItemSilhouette(graphics, stack, x + 1, y + 1);

                    if (isHovering) {
                        graphics.flush();
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                        drawOutline(graphics, stack, x, y);
                    }
                    else if (Tide.CLIENT_CONFIG.journal.showUnread && (isUnlocked || hasNote) && isUnread) {
                        graphics.flush();
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1f, 0.88f, 0f, 1f);
                        drawOutline(graphics, stack, x, y);
                        RenderSystem.disableBlend();
                    }

                    graphics.flush();
                    if (isUnlocked) {
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                        graphics.renderItem(stack, x, y);
                    }
                    else {
                        RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
                        graphics.renderItem(stack, x, y);
                        graphics.flush();
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    }

                    if (!isUnlocked && hasNote) {
                        graphics.pose().pushPose();
                        graphics.pose().translate(0f, 0f, 200f);
                        if (isHovering) graphics.blit(FISHY_NOTE_ICON_SELECTED, x -2, y - 2,
                                0f, 0f, 9, 9, 9, 9);
                        else graphics.blit(FISHY_NOTE_ICON, x - 2, y - 2,
                                0f, 0f, 9, 9, 9, 9);
                        graphics.pose().popPose();
                    }

                    i++;
                }

                cursorY += Mth.floor((float) (i - 1) / FISH_PER_ROW + 1) * CELL_SIZE + GROUP_SPACING;

                groupIndex++;
                if (groupIndex < fishByPage.get(page).size())
                    graphics.blitSprite(LINE_BOTTOM, tlX + 40, tlY + 36 + cursorY - GROUP_SPACING / 2, 327, 2);
            }
        }

        if (xButton != null) xButton.render(graphics, mouseX, mouseY, partialTick);
        if (leftButton != null) leftButton.render(graphics, mouseX, mouseY, partialTick);
        if (rightButton != null) rightButton.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();

        if (updatePage) pageChanged();
        this.didClick = false;
    }

    private void drawOutline(@org.jetbrains.annotations.NotNull GuiGraphics graphics, ItemStack stack, int x, int y) {
        final int[][] offs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (int[] off : offs) FishingJournal.renderItemSilhouette(graphics, stack, x + off[0], y + off[1]);
    }

}
