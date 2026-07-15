package com.kingdomrp.core.client.screen.widget;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

/** Виджет-панель прогресса путей: 5 полос XP (порт рендера старого PathScreen). */
public class PathPanelWidget extends AbstractSpruceWidget {

    public PathPanelWidget(Position position, int width, int height) {
        super(position);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Font font = Minecraft.getInstance().font;
        PlayerData data = player.getData(KRPAttachments.PLAYER_DATA);

        int x = getX();
        int y = getY();
        for (Path path : Path.values()) {
            renderPath(graphics, font, data, path, x, y + path.index * 36);
        }
    }

    private void renderPath(GuiGraphics graphics, Font font, PlayerData data, Path path, int x, int y) {
        String name = XPSystem.getPathName(path);
        int level = data.getLevel(path);
        float xp = data.getXP(path);
        float required = data.getXPRequired(path);
        int color = getPathColor(path);
        float mult = data.getXPMultiplier(path);

        graphics.drawString(font, "§f" + getPathIcon(path) + " " + name, x, y, color);
        graphics.drawString(font, "§7Ур. §f" + level, x + 150, y, 0xFFFFFF);
        if (mult < 1f) {
            graphics.drawString(font, "§7XP: §f" + String.format("%.0f%%", mult * 100), x + 200, y, 0xFFFFFF);
        }

        int barX = x, barY = y + 12, barW = 250, barH = 8;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        int filled = required > 0 ? (int) ((xp / required) * barW) : 0;
        if (filled > 0) graphics.fill(barX, barY, barX + filled, barY + barH, color);
        graphics.fill(barX, barY, barX + barW, barY + 1, 0xFF666666);
        graphics.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF666666);
        graphics.fill(barX, barY, barX + 1, barY + barH, 0xFF666666);
        graphics.fill(barX + barW - 1, barY, barX + barW, barY + barH, 0xFF666666);
        graphics.drawString(font, String.format("%.0f / %.0f XP", xp, required), barX + 2, barY + 1, 0xFFFFFFFF);
    }

    private int getPathColor(Path path) {
        return switch (path) {
            case CRAFT -> 0xFFE67E22;
            case HARVEST -> 0xFF2ECC71;
            case MINING -> 0xFF95A5A6;
            case WAR -> 0xFFE74C3C;
            case MAGIC -> 0xFF9B59B6;
        };
    }

    private String getPathIcon(Path path) {
        return switch (path) {
            case CRAFT -> "⚒";
            case HARVEST -> "🍖";
            case MINING -> "⛏";
            case WAR -> "⚔";
            case MAGIC -> "✨";
        };
    }
}
