package com.kingdomrp.core.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

import java.util.function.IntConsumer;

/**
 * HSV-палитра: квадрат насыщенность/яркость + полоса оттенка. Полный спектр,
 * не фиксированные цвета. {@link #getColor()} — выбранный RGB.
 */
public class ColorPickerWidget extends AbstractSpruceWidget {

    private static final int HUE_BAR_W = 14;
    private static final int GAP = 6;
    private static final int STEPS = 24;

    private float hue, sat = 1f, val = 1f;
    private final IntConsumer onChange;

    public ColorPickerWidget(Position position, int width, int height, int initialRgb, IntConsumer onChange) {
        super(position);
        this.width = width;
        this.height = height;
        this.onChange = onChange;
        this.active = true;
        setFromRgb(initialRgb);
    }

    private int square() { return Math.min(this.height, this.width - HUE_BAR_W - GAP); }

    public int getColor() { return hsvToRgb(hue, sat, val); }

    public void setFromRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        float max = Math.max(r, Math.max(g, b)) / 255f;
        float min = Math.min(r, Math.min(g, b)) / 255f;
        float d = max - min;
        val = max;
        sat = max == 0 ? 0 : d / max;
        float h = 0;
        if (d != 0) {
            float rf = r / 255f, gf = g / 255f, bf = b / 255f;
            if (max == rf) h = ((gf - bf) / d) % 6;
            else if (max == gf) h = (bf - rf) / d + 2;
            else h = (rf - gf) / d + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        hue = h;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), sq = square();
        float cell = sq / (float) STEPS;

        // Квадрат S/V для текущего оттенка.
        for (int i = 0; i < STEPS; i++) {
            for (int j = 0; j < STEPS; j++) {
                int c = hsvToRgb(hue, i / (float) (STEPS - 1), 1f - j / (float) (STEPS - 1));
                int cx = x + (int) (i * cell), cy = y + (int) (j * cell);
                g.fill(cx, cy, x + (int) ((i + 1) * cell), y + (int) ((j + 1) * cell), 0xFF000000 | c);
            }
        }
        // Полоса оттенка.
        int barX = x + sq + GAP;
        float hcell = sq / (float) STEPS;
        for (int k = 0; k < STEPS; k++) {
            int c = hsvToRgb(k / (float) (STEPS - 1), 1f, 1f);
            g.fill(barX, y + (int) (k * hcell), barX + HUE_BAR_W, y + (int) ((k + 1) * hcell), 0xFF000000 | c);
        }

        // Маркеры.
        int mx = x + (int) (sat * sq), my = y + (int) ((1f - val) * sq);
        g.fill(mx - 2, my - 2, mx + 2, my + 2, 0xFFFFFFFF);
        int hy = y + (int) (hue * sq);
        g.fill(barX - 1, hy - 1, barX + HUE_BAR_W + 1, hy + 1, 0xFFFFFFFF);
    }

    @Override
    protected boolean onMouseClick(double mx, double my, int button) {
        return apply(mx, my);
    }

    @Override
    protected boolean onMouseDrag(double mx, double my, int button, double dx, double dy) {
        return apply(mx, my);
    }

    private boolean apply(double mx, double my) {
        int x = getX(), y = getY(), sq = square();
        if (mx >= x && mx < x + sq && my >= y && my < y + sq) {
            sat = clamp((float) (mx - x) / sq);
            val = 1f - clamp((float) (my - y) / sq);
            onChange.accept(getColor());
            return true;
        }
        int barX = x + sq + GAP;
        if (mx >= barX && mx < barX + HUE_BAR_W && my >= y && my < y + sq) {
            hue = clamp((float) (my - y) / sq);
            onChange.accept(getColor());
            return true;
        }
        return false;
    }

    private static float clamp(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    /** HSV → RGB (без альфы). */
    public static int hsvToRgb(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
