package com.example.blockinfo.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 的排版与绘制。
 *
 * <p>这里刻意只使用 {@link GuiGraphics#fill(int, int, int, int, int)} 与
 * {@link GuiGraphics#drawString(Font, String, int, int, int)} —— 这两个接口在整条
 * Minecraft 1.21.x 线上都稳定。原先用的
 * {@code RenderSystem.setShaderTexture/setShaderColor} + {@code GuiGraphics.blit}
 * 从 1.21.2 起经历了多轮重写（blit 签名变化、贴图绑定方式变化），是跨版本编译时
 * 最大的障碍；进度条改成纯色填充后就不再依赖它们了。
 */
public final class BlockInfoHud {
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_WIDTH = 40;
    private static final float SMOOTHING_FACTOR = 0.15f;

    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_PLANT = 0x55FF55;
    private static final int COLOR_COOK = 0xFFD700;
    private static final int COLOR_BURN = 0xFF6600;
    private static final int COLOR_MINE = 0x55FFFF;

    private static float currentBoxWidth = 0f;
    private static float currentBoxHeight = 0f;

    private BlockInfoHud() {
    }

    public static void render(GuiGraphics gui, Font font, int screenWidth, BlockInfoData data) {
        if (data.isFurnace) {
            float cookProgress = 0f;
            if (data.furnaceTotalCookTime > 0) {
                cookProgress = (float) data.furnaceCookTime / data.furnaceTotalCookTime;
            }
            data.cookProgress = clamp01(cookProgress);

            float burnProgress = 0f;
            if (data.furnaceTotalBurnTime > 0) {
                burnProgress = (float) data.furnaceBurnTime / data.furnaceTotalBurnTime;
            }
            data.burnProgress = clamp01(burnProgress);
        }

        List<String> headerLines = new ArrayList<>();
        headerLines.add(data.blockName);
        headerLines.add("[" + data.modId + "]");

        List<String> extraLines = new ArrayList<>();
        if (data.isFurnace) {
            if (!data.inputItemName.isEmpty()) {
                extraLines.add(tr("blockinfo.hud.input", data.inputItemName, data.inputItemCount));
            }
            if (!data.fuelItemName.isEmpty()) {
                extraLines.add(tr("blockinfo.hud.fuel", data.fuelItemName, data.fuelItemCount));
            }
            if (!data.outputItemName.isEmpty()) {
                extraLines.add(tr("blockinfo.hud.output", data.outputItemName, data.outputItemCount));
            }
        }

        boolean hasContainerItems = data.isContainer
                && data.containerItems != null && !data.containerItems.isEmpty();
        if (hasContainerItems) {
            extraLines.add(tr("blockinfo.hud.contents"));
        }

        int itemsPerColumn = 5;
        int containerNumColumns = 1;
        int containerColumnWidth = 0;
        int containerMaxRows = 0;

        if (hasContainerItems) {
            int numItems = data.containerItems.size();
            if (numItems > 15) {
                itemsPerColumn = 8;
            } else if (numItems > 10) {
                itemsPerColumn = 6;
            }
            containerNumColumns = (numItems + itemsPerColumn - 1) / itemsPerColumn;
            for (String item : data.containerItems) {
                int w = font.width("  - " + item);
                if (w > containerColumnWidth) {
                    containerColumnWidth = w;
                }
            }
            containerMaxRows = Math.min(itemsPerColumn, numItems);
        }

        int maxLineWidth = 0;
        for (String line : headerLines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        String mineIcon = data.canMine ? "\u2713" : "\u2717";
        maxLineWidth += font.width(mineIcon) + 8;
        for (String line : extraLines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        // 进度条那一行由 isMining 决定，而不是 mineProgress > 0：进度是 0..10 的整数档位，
        // 用数值判断会让刚开始挖（0%）和连挖破块的空隙反复切换，HUD 就会闪。
        boolean showMiningBar = data.isMining;
        boolean anyBar = data.isPlant || data.isFurnace || showMiningBar;
        if (anyBar) {
            maxLineWidth += BAR_WIDTH + 12;
        }

        int containerWidth = containerNumColumns * (containerColumnWidth + 10);
        if (containerWidth > maxLineWidth) {
            maxLineWidth = containerWidth;
        }

        int boxHeight = (headerLines.size() + extraLines.size()) * LINE_HEIGHT + PADDING * 2;
        if (data.isPlant) {
            boxHeight += BAR_HEIGHT + 6;
        }
        if (data.isFurnace) {
            boxHeight += (BAR_HEIGHT + 6) * 2;
        }
        if (showMiningBar) {
            boxHeight += BAR_HEIGHT + 6;
        }
        if (hasContainerItems) {
            boxHeight += containerMaxRows * LINE_HEIGHT;
        }

        int targetBoxWidth = maxLineWidth + PADDING * 2;

        currentBoxWidth += (targetBoxWidth - currentBoxWidth) * SMOOTHING_FACTOR;
        currentBoxHeight += (boxHeight - currentBoxHeight) * SMOOTHING_FACTOR;

        int boxWidth = (int) currentBoxWidth;
        int boxHeightNow = (int) currentBoxHeight;
        int x = (screenWidth - boxWidth) / 2;
        int y = PADDING;

        gui.fill(x, y, x + boxWidth, y + boxHeightNow, 0x80000000);
        gui.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeightNow - 1, 0x60000000);

        int currentY = y + PADDING;

        gui.text(font, data.blockName, x + PADDING, currentY, COLOR_TEXT);
        currentY += LINE_HEIGHT;

        int modIdWidth = font.width("[" + data.modId + "]");
        gui.text(font, "[" + data.modId + "]", x + PADDING, currentY, COLOR_TEXT);
        gui.text(font, mineIcon, x + PADDING + modIdWidth + 8, currentY,
                data.canMine ? COLOR_PLANT : 0xFF5555);
        currentY += LINE_HEIGHT;

        if (data.isPlant) {
            currentY += 2;
            drawBar(gui, x + PADDING, currentY, data.plantGrowth, COLOR_PLANT);
            gui.text(font, BlockInfoUtil.formatPercentage(data.plantGrowth),
                    x + PADDING + BAR_WIDTH + 8, currentY + 1, COLOR_PLANT);
            currentY += BAR_HEIGHT + 4;
        }

        if (data.isFurnace) {
            currentY += 2;
            currentY = drawLabelledBar(gui, font, x + PADDING, currentY, tr("blockinfo.hud.cooking"), data.cookProgress, COLOR_COOK);
            currentY = drawLabelledBar(gui, font, x + PADDING, currentY, tr("blockinfo.hud.burning"), data.burnProgress, COLOR_BURN);
        }

        if (showMiningBar) {
            currentY += 2;
            currentY = drawLabelledBar(gui, font, x + PADDING, currentY, tr("blockinfo.hud.mining"), data.mineProgress, COLOR_MINE);
        }

        for (String line : extraLines) {
            gui.text(font, line, x + PADDING, currentY, COLOR_TEXT);
            currentY += LINE_HEIGHT;
        }

        if (hasContainerItems) {
            int numItems = data.containerItems.size();
            int startX = x + PADDING;
            int startY = currentY;

            for (int col = 0; col < containerNumColumns; col++) {
                int colX = startX + col * (containerColumnWidth + 10);
                int colY = startY;

                for (int row = 0; row < itemsPerColumn; row++) {
                    int index = col * itemsPerColumn + row;
                    if (index >= numItems) {
                        break;
                    }
                    gui.text(font, "  - " + data.containerItems.get(index), colX, colY, COLOR_TEXT);
                    colY += LINE_HEIGHT;
                }
            }
        }
    }

    private static int drawLabelledBar(GuiGraphics gui, Font font, int x, int y,
                                       String label, float progress, int color) {
        gui.text(font, label, x, y, color);
        int barX = x + font.width(label) + 6;
        drawBar(gui, barX, y, progress, color);
        gui.text(font, BlockInfoUtil.formatPercentage(progress),
                barX + BAR_WIDTH + 8, y + 1, color);
        return y + BAR_HEIGHT + 6;
    }

    private static void drawBar(GuiGraphics gui, int x, int y, float progress, int color) {
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000);
        gui.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, 0xFF303030);

        int filled = Math.round((BAR_WIDTH - 2) * clamp01(progress));
        if (filled > 0) {
            gui.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, color);
        }
    }

    /** 取翻译后的字符串；lang 文件缺失时 Component 会退化成 key 本身。 */
    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
    private static float clamp01(float value) {
        return Math.min(Math.max(value, 0f), 1f);
    }
}
