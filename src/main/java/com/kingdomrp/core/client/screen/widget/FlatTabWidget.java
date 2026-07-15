package com.kingdomrp.core.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

/** Плоская вкладка в стиле ObsidianUI (не 3D-кнопка). Прилегает к панели снизу. */
public class FlatTabWidget extends AbstractSpruceWidget {

    private static final int ACCENT = 0xFFAA8855;

    private final Component label;
    private final Runnable onClick;
    private boolean selected;

    public FlatTabWidget(Position position, int width, int height, Component label, Runnable onClick) {
        super(position);
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.active = true;
    }

    public void setSelected(boolean selected) { this.selected = selected; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = this.width, h = this.height;
        boolean hover = isMouseHovered();

        int bg = selected ? 0xFF2A2A2A : (hover ? 0xC0242424 : 0xC0141414);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, ACCENT);          // верх
        g.fill(x, y, x + 1, y + h, ACCENT);          // лево
        g.fill(x + w - 1, y, x + w, y + h, ACCENT);  // право
        if (!selected) g.fill(x, y + h - 1, x + w, y + h, ACCENT);   // низ — только у невыбранной (разделитель)

        int color = selected ? 0xFFFFD700 : (hover ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.drawCenteredString(font, label, x + w / 2, y + (h - font.lineHeight) / 2 + 1, color);
    }

    @Override
    protected boolean onMouseClick(double mx, double my, int button) {
        onClick.run();
        return true;
    }
}
