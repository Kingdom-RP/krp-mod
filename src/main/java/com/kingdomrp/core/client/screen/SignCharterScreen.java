package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.network.SignCharterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.screen.SpruceScreen;
import org.thinkingstudio.obsidianui.widget.SpruceButtonWidget;
import org.thinkingstudio.obsidianui.widget.SpruceLabelWidget;
import org.thinkingstudio.obsidianui.widget.text.SpruceTextFieldWidget;

/** Экран подписи хартии: ввод названия королевства → {@link SignCharterPacket}. */
public class SignCharterScreen extends SpruceScreen {

    private SpruceTextFieldWidget nameField;

    public SignCharterScreen() {
        super(Component.translatable("kingdomrp.sign.title"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(new SpruceLabelWidget(Position.of(this, cx - 100, cy - 40),
                Component.translatable("kingdomrp.sign.prompt"), 200, true));

        this.nameField = new SpruceTextFieldWidget(Position.of(this, cx - 100, cy - 20), 200, 20,
                Component.translatable("kingdomrp.sign.name"));
        this.addRenderableWidget(this.nameField);

        this.addRenderableWidget(new SpruceButtonWidget(Position.of(this, cx - 100, cy + 10), 95, 20,
                Component.translatable("kingdomrp.sign.confirm"), btn -> confirm()).asVanilla());
        this.addRenderableWidget(new SpruceButtonWidget(Position.of(this, cx + 5, cy + 10), 95, 20,
                CommonComponents.GUI_CANCEL, btn -> this.onClose()).asVanilla());
    }

    private void confirm() {
        String name = this.nameField.getText().trim();
        if (name.isEmpty()) return;
        PacketDistributor.sendToServer(new SignCharterPacket(name));
        this.onClose();
    }

    @Override
    public void renderTitle(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }
}
