package com.kingdomrp.core.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;
import org.thinkingstudio.obsidianui.widget.text.SpruceTextFieldWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Живой автоподбор ников: читает текущий текст поля каждый кадр (без rebuild),
 * рисует до 5 подходящих онлайн-игроков, клик выбирает.
 */
public class InviteSuggestWidget extends AbstractSpruceWidget {

    private static final int ROW_H = 14;
    private static final int MAX = 5;

    private final SpruceTextFieldWidget field;
    private final Supplier<List<String>> excluded;   // жители (не предлагать)
    private final Consumer<String> onPick;

    public InviteSuggestWidget(Position position, int width, SpruceTextFieldWidget field,
                               Supplier<List<String>> excluded, Consumer<String> onPick) {
        super(position);
        this.width = width;
        this.height = ROW_H * MAX;
        this.field = field;
        this.excluded = excluded;
        this.onPick = onPick;
        this.active = true;
    }

    private List<String> matches() {
        List<String> out = new ArrayList<>();
        String q = field.getText().trim().toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        if (q.isEmpty() || mc.getConnection() == null) return out;
        String self = mc.getUser().getName();
        List<String> excl = excluded.get();
        for (var pi : mc.getConnection().getOnlinePlayers()) {
            String n = pi.getProfile().getName();
            if (n.equalsIgnoreCase(self)) continue;
            if (excl.stream().anyMatch(m -> m.equalsIgnoreCase(n))) continue;
            if (n.toLowerCase(Locale.ROOT).startsWith(q) && !n.equalsIgnoreCase(q)) out.add(n);
            if (out.size() >= MAX) break;
        }
        return out;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        List<String> m = matches();
        int x = getX(), y = getY();
        for (int i = 0; i < m.size(); i++) {
            int ry = y + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + width && mouseY >= ry && mouseY < ry + ROW_H;
            g.fill(x, ry, x + width, ry + ROW_H, hover ? 0xFF3A3A3A : 0xC0202020);
            g.drawString(font, m.get(i), x + 4, ry + 3, hover ? 0xFFFFFF00 : 0xFFCCCCCC);
        }
    }

    @Override
    protected boolean onMouseClick(double mx, double my, int button) {
        List<String> m = matches();
        int x = getX(), y = getY();
        if (mx < x || mx >= x + width) return false;
        int row = (int) ((my - y) / ROW_H);
        if (row >= 0 && row < m.size()) {
            onPick.accept(m.get(row));
            return true;
        }
        return false;
    }
}
