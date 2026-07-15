package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.client.screen.widget.ColorPickerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.screen.SpruceScreen;
import org.thinkingstudio.obsidianui.widget.SpruceButtonWidget;

import javax.annotation.Nullable;
import java.util.function.IntConsumer;

/** Экран выбора цвета (HSV-палитра) с подтверждением. Переиспользуемый. */
public class ColorPickerScreen extends SpruceScreen {

    @Nullable private final Screen parent;
    private final IntConsumer onConfirm;
    private int color;
    private ColorPickerWidget picker;

    public ColorPickerScreen(@Nullable Screen parent, Component title, int initialRgb, IntConsumer onConfirm) {
        super(title);
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.color = initialRgb;
    }

    @Override
    protected void init() {
        super.init();
        int pw = 220, ph = 130;
        int px = this.width / 2 - pw / 2;
        int py = this.height / 2 - ph / 2 - 10;

        this.picker = new ColorPickerWidget(Position.of(px, py), pw, ph, this.color, c -> this.color = c);
        this.addRenderableWidget(this.picker);

        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(this.width / 2 - 100, py + ph + 12), 95, 20,
                Component.translatable("kingdomrp.color.confirm"), btn -> {
                    onConfirm.accept(this.color);
                    close();
                }).asVanilla());
        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(this.width / 2 + 5, py + ph + 12), 95, 20,
                CommonComponents.GUI_CANCEL, btn -> close()).asVanilla());
    }

    private void close() { Minecraft.getInstance().setScreen(parent); }

    @Override
    public void renderTitle(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 85, 0xFFFFFF);
        // Превью текущего цвета.
        int bx = this.width / 2 - 10;
        g.fill(bx, this.height / 2 - 75, bx + 20, this.height / 2 - 65, 0xFF000000 | this.color);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
