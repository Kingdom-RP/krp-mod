package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.client.ClientKingdomData;
import com.kingdomrp.core.kingdom.CharterData;
import com.kingdomrp.core.kingdom.block.KingdomMenu;
import com.kingdomrp.core.kingdom.item.CharterItem;
import com.kingdomrp.core.kingdom.upkeep.Characteristic;
import com.kingdomrp.core.network.CreateKingdomPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Экран блока королевства: создание (хартия+«Основать») или содержание (слоты+шкалы). */
public class KingdomBlockScreen extends AbstractContainerScreen<KingdomMenu> {

    private static final int PANEL     = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW    = 0xFF555555;
    private static final int SLOT_BG   = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int[] BAR_COLORS = {0xFF2ECC71, 0xFFB5651D, 0xFF3498DB};

    private static final int BAR_X = 30, BAR_W = 108, BAR_H = 9;
    private static final int HELP_X = 152;

    private Button createButton;

    public KingdomBlockScreen(KingdomMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = menu.isActive() ? 206 : 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (!this.menu.isActive()) {
            this.createButton = this.addRenderableWidget(Button.builder(
                            Component.translatable("kingdomrp.block.create"), btn -> openColorPicker())
                    .bounds(this.leftPos + this.imageWidth / 2 - 32, this.topPos + 44, 64, 20).build());
        }
    }

    private void openColorPicker() {
        int[] pal = com.kingdomrp.core.kingdom.KingdomManager.COLOR_PALETTE;
        int initial = pal[(int) (Math.random() * pal.length)];
        var pos = this.menu.getPos();
        net.minecraft.client.Minecraft.getInstance().setScreen(new ColorPickerScreen(
                null, Component.translatable("kingdomrp.color.pick_title"), initial,
                color -> PacketDistributor.sendToServer(new CreateKingdomPacket(pos, color))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.createButton != null) this.createButton.active = readyToCreate();
    }

    private boolean readyToCreate() {
        CharterData d = CharterItem.data(this.menu.getCharter());
        return d != null && d.readyToCreate();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos, w = this.imageWidth, h = this.imageHeight;
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, HIGHLIGHT);
        g.fill(x, y, x + 1, y + h, HIGHLIGHT);
        g.fill(x, y + h - 1, x + w, y + h, SHADOW);
        g.fill(x + w - 1, y, x + w, y + h, SHADOW);
        for (Slot slot : this.menu.slots) drawSlot(g, x + slot.x, y + slot.y);

        if (this.menu.isActive()) renderCharacteristics(g);
    }

    private void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, HIGHLIGHT);
        g.fill(sx - 1, sy - 1, sx + 16, sy + 16, SLOT_DARK);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
    }

    private void renderCharacteristics(GuiGraphics g) {
        var info = ClientKingdomData.get();
        float[] vals = {info.food(), info.materials(), info.prosperity()};
        String[] keys = {"kingdomrp.upkeep.food", "kingdomrp.upkeep.materials", "kingdomrp.upkeep.prosperity"};
        for (int i = 0; i < 3; i++) {
            int rowY = this.topPos + KingdomMenu.RES_SLOT_Y[i];
            g.drawString(this.font, Component.translatable(keys[i]),
                    this.leftPos + BAR_X, rowY - 2, 0xFF303030, false);
            drawSegBar(g, this.leftPos + BAR_X, rowY + 8, vals[i], Characteristic.MAX, BAR_COLORS[i]);
            g.drawString(this.font, "?", this.leftPos + HELP_X + 2, rowY + 4, 0xFF3060B0, false);
        }
        g.drawString(this.font, "?", this.leftPos + HELP_X + 2, this.topPos + 5, 0xFF3060B0, false);
    }

    /** Сегментированная шкала (как опыт): 10 делений по 100. */
    private void drawSegBar(GuiGraphics g, int x, int y, float value, float max, int color) {
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF000000);
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF555555);
        int filled = (int) (Math.max(0f, Math.min(1f, value / max)) * BAR_W);
        if (filled > 0) g.fill(x, y, x + filled, y + BAR_H, color);
        for (int s = 1; s < 10; s++) {
            int sx = x + (int) (BAR_W * s / 10f);
            g.fill(sx, y, sx + 1, y + BAR_H, 0xFF303030);
        }
        String txt = String.valueOf((int) value);
        g.drawString(this.font, txt, x + BAR_W / 2 - this.font.width(txt) / 2, y + 1, 0xFFFFFFFF, true);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF303030, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF303030, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderHelpTooltips(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private void renderHelpTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (!this.menu.isActive()) return;
        // Общий тултип (верх-право).
        if (over(mouseX, mouseY, this.leftPos + HELP_X, this.topPos + 4)) {
            g.renderComponentTooltip(this.font, lines(
                    "kingdomrp.upkeep.help.title", "kingdomrp.upkeep.help.1",
                    "kingdomrp.upkeep.help.2", "kingdomrp.upkeep.help.3", "kingdomrp.upkeep.help.4"),
                    mouseX, mouseY);
            return;
        }
        String[] names = {"food", "materials", "prosperity"};
        for (int i = 0; i < 3; i++) {
            int iy = this.topPos + KingdomMenu.RES_SLOT_Y[i] + 3;
            if (over(mouseX, mouseY, this.leftPos + HELP_X, iy)) {
                g.renderComponentTooltip(this.font, lines(
                        "kingdomrp.upkeep." + names[i],
                        "kingdomrp.upkeep." + names[i] + ".consume",
                        "kingdomrp.upkeep." + names[i] + ".refill"), mouseX, mouseY);
                return;
            }
        }
    }

    private boolean over(int mx, int my, int x, int y) {
        return mx >= x && mx < x + 11 && my >= y && my < y + 11;
    }

    private List<Component> lines(String... keys) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        for (int i = 0; i < keys.length; i++)
            out.add(Component.translatable(keys[i]).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        return out;
    }
}
