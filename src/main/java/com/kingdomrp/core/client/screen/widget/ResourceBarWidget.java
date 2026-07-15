package com.kingdomrp.core.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

/** Шкала характеристики содержания: подпись + level-bar со значением. */
public class ResourceBarWidget extends AbstractSpruceWidget {

    private final Component label;
    private final float value, max;
    private final int color;

    public ResourceBarWidget(Position position, int width, Component label, float value, float max, int color) {
        super(position);
        this.width = width;
        this.height = 22;
        this.label = label;
        this.value = value;
        this.max = max;
        this.color = color;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = this.width;
        g.drawString(font, label, x, y, 0xFFFFFFFF);

        int by = y + 11, h = 8;
        g.fill(x - 1, by - 1, x + w + 1, by + h + 1, 0xFF000000);
        g.fill(x, by, x + w, by + h, 0xFF555555);
        int filled = (int) (Math.max(0f, Math.min(1f, value / max)) * w);
        if (filled > 0) g.fill(x, by, x + filled, by + h, color);
        for (int s = 1; s < 10; s++) {   // 10 делений по 100
            int sx = x + (int) (w * s / 10f);
            g.fill(sx, by, sx + 1, by + h, 0xFF303030);
        }
        String txt = (int) value + " / " + (int) max;
        g.drawString(font, txt, x + w - font.width(txt) - 2, by, 0xFFFFFFFF);
    }
}
