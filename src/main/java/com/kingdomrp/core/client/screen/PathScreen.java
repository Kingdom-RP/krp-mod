package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PathScreen extends Screen {

    private static final int BG_WIDTH    = 280;
    private static final int BASE_HEIGHT = 220;
    private static final int BUTTON_H    = 22;

    public PathScreen() {
        super(Component.literal("Пути развития"));
    }

    // Выносим построение кнопок отдельно чтобы корректно считать btnY
    private void rebuildButtons(int x) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        int[] count = {0};
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            for (Path path : Path.values()) {
                if (data.hasAvailablePoints(path)) count[0]++;
            }
        });

        int totalHeight = BASE_HEIGHT + count[0] * BUTTON_H;
        int y = (this.height - totalHeight) / 2;
        int[] btnY = {y + BASE_HEIGHT};

        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            for (Path path : Path.values()) {
                if (data.hasAvailablePoints(path)) {
                    this.addRenderableWidget(Button.builder(
                                    Component.literal("§6► " + XPSystem.getPathName(path)
                                            + " — выбрать специализацию ◄"),
                                    btn -> Minecraft.getInstance().setScreen(
                                            new SpecializationScreen(path)))
                            .pos(x + 10, btnY[0])
                            .size(BG_WIDTH - 20, 20)
                            .build());
                    btnY[0] += BUTTON_H;
                }
            }
        });
    }

    @Override
    protected void init() {
        int x = (this.width - BG_WIDTH) / 2;
        rebuildButtons(x);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        int[] count = {0};
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            for (Path path : Path.values()) {
                if (data.hasAvailablePoints(path)) count[0]++;
            }
        });

        int totalHeight = BASE_HEIGHT + count[0] * BUTTON_H;
        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - totalHeight) / 2;

        graphics.fill(x, y, x + BG_WIDTH, y + totalHeight, 0xCC1A1A1A);
        graphics.fill(x, y, x + BG_WIDTH, y + 1, 0xFFAA8855);
        graphics.fill(x, y + totalHeight - 1, x + BG_WIDTH, y + totalHeight, 0xFFAA8855);
        graphics.fill(x, y, x + 1, y + totalHeight, 0xFFAA8855);
        graphics.fill(x + BG_WIDTH - 1, y, x + BG_WIDTH, y + totalHeight, 0xFFAA8855);

        graphics.drawCenteredString(this.font, "§6⚔ Пути развития ⚔",
                this.width / 2, y + 10, 0xFFFFFF);
        graphics.fill(x + 10, y + 22, x + BG_WIDTH - 10, y + 23, 0xFFAA8855);

        if (count[0] > 0) {
            graphics.fill(x + 10, y + BASE_HEIGHT - 2,
                    x + BG_WIDTH - 10, y + BASE_HEIGHT - 1, 0xFF555555);
        }

        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            for (Path path : Path.values()) {
                renderPath(graphics, data, path, x + 15, y + 32 + path.index * 36);
            }
        });

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPath(GuiGraphics graphics, PlayerData data,
                            Path path, int x, int y) {
        String name     = XPSystem.getPathName(path);
        int    level    = data.getLevel(path);
        float  xp       = data.getXP(path);
        float  required = data.getXPRequired(path);
        int    color    = getPathColor(path);
        float  mult     = data.getXPMultiplier(path);

        graphics.drawString(this.font,
                "§f" + getPathIcon(path) + " " + name, x, y, color);
        graphics.drawString(this.font,
                "§7Ур. §f" + level, x + 150, y, 0xFFFFFF);

        if (mult < 1f) {
            graphics.drawString(this.font,
                    "§7XP: §f" + String.format("%.0f%%", mult * 100),
                    x + 200, y, 0xFFFFFF);
        }

        int barX = x, barY = y + 12, barW = 250, barH = 8;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);

        int filled = required > 0 ? (int) ((xp / required) * barW) : 0;
        if (filled > 0) {
            graphics.fill(barX, barY, barX + filled, barY + barH, color);
        }

        graphics.fill(barX, barY, barX + barW, barY + 1, 0xFF666666);
        graphics.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF666666);
        graphics.fill(barX, barY, barX + 1, barY + barH, 0xFF666666);
        graphics.fill(barX + barW - 1, barY, barX + barW, barY + barH, 0xFF666666);

        graphics.drawString(this.font,
                String.format("%.0f / %.0f XP", xp, required),
                barX + 2, barY + 1, 0xFFFFFFFF);
    }

    private int getPathColor(Path path) {
        return switch (path) {
            case CRAFT   -> 0xFFE67E22;
            case HARVEST -> 0xFF2ECC71;
            case MINING  -> 0xFF95A5A6;
            case WAR     -> 0xFFE74C3C;
            case MAGIC   -> 0xFF9B59B6;
        };
    }

    private String getPathIcon(Path path) {
        return switch (path) {
            case CRAFT   -> "⚒";
            case HARVEST -> "🍖";
            case MINING  -> "⛏";
            case WAR     -> "⚔";
            case MAGIC   -> "✨";
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}