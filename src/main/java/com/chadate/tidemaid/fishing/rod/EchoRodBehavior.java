package com.chadate.tidemaid.fishing.rod;

import com.chadate.tidemaid.entity.TideMaidFishingHook;
import com.li64.tide.data.fishing.CatchResult;
import com.li64.tide.data.fishing.CrateData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class EchoRodBehavior implements RodBehavior {

    @Override
    public void onNibble(TideMaidFishingHook hook, ServerLevel level) {
        level.playSound(
                null, hook.blockPosition(),
                SoundEvents.SCULK_CLICKING, SoundSource.MASTER, 1.0f,
                1.0f - (hook.getRandom().nextFloat() - hook.getRandom().nextFloat()) * 0.1f);
    }

    @Override
    public void onCatch(TideMaidFishingHook hook, CatchResult result) {
        ItemStack preview = null;

        if (result.isFish() || result.isLoot()) {
            // FISH/ITEM 类型：显示第一个物品
            if (!result.items().isEmpty()) {
                preview = result.items().get(0).copy();
            }
        } else if (result.isCrate()) {
            // CRATE 类型：获取宝箱方块对应的物品
            result.entry().ifPresent(entry -> {
                if (entry instanceof CrateData crateData) {
                    ItemStack crateItem = new ItemStack(
                            crateData.blockProvider().getState(hook.getRandom(), hook.blockPosition()).getBlock().asItem()
                    );
                    hook.setPreviewItem(crateItem);
                    hook.showFishChatBubble(crateItem);
                }
            });
        }
        // NOTHING 类型：不显示预览

        if (preview != null) {
            hook.setPreviewItem(preview);
            hook.showFishChatBubble(preview);
        }
    }

    @Override
    public void onRetrieve(TideMaidFishingHook hook) {
        // 清理预览物品
        hook.clearPreviewItem();
    }
}
