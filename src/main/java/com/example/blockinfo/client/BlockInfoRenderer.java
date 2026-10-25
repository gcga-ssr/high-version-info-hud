package com.example.blockinfo.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 只负责订阅 GUI 渲染事件，真正的排版与绘制在 {@link BlockInfoHud}。
 *
 * <p>{@code RenderGuiEvent.Post} 在 NeoForge 21.0 ~ 21.11 全部版本线里都存在且有
 * {@code getGuiGraphics()}，所以这个文件在所有目标版本上通用。
 */
public class BlockInfoRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        BlockInfoData data = BlockInfoTracker.getCurrentData();
        if (data == null || data.blockName.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BlockInfoHud.render(event.getGuiGraphics(), mc.font, mc.getWindow().getGuiScaledWidth(), data);
    }
}
