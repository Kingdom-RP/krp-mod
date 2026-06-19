package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.network.ChooseSpecializationPacket;
import com.kingdomrp.core.specialization.Specialization;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SpecializationScreen extends Screen {

    private static final int BG_WIDTH  = 300;
    private static final int BG_HEIGHT = 260;
    private static final int PADDING   = 12;

    // Считаем Y-позиции кнопок один раз, используем и в init и в render
    private int[] buttonYPositions = new int[0];
    private int calculatedHeight = BG_HEIGHT;

    private void calculateLayout() {
        int currentY = 40; // отступ от заголовка
        buttonYPositions = new int[specs.size()];

        for (int i = 0; i < specs.size(); i++) {
            buttonYPositions[i] = currentY;
            currentY += 22; // высота кнопки

            // Считаем сколько строк займёт описание
            String desc = specs.get(i).getDescription();
            var lines = this.font.split(
                    net.minecraft.network.chat.Component.literal(desc),
                    BG_WIDTH - PADDING * 2
            );
            currentY += lines.size() * 10 + 8; // строки + отступ снизу
        }

        calculatedHeight = Math.max(BG_HEIGHT, currentY + 20);
    }

    private final Path path;
    private final List<Specialization> specs;

    public SpecializationScreen(Path path) {
        super(Component.literal("Выбор специализации"));
        this.path = path;
        this.specs = SpecializationRegistry.getForPath(path);
    }

    @Override
    protected void init() {
        calculateLayout();

        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - calculatedHeight) / 2;

        for (int i = 0; i < specs.size(); i++) {
            Specialization spec = specs.get(i);
            boolean canAfford = canAfford(spec);

            this.addRenderableWidget(Button.builder(
                            Component.literal(spec.getName()),
                            btn -> chooseSpecialization(spec.getId()))
                    .pos(x + PADDING, y + buttonYPositions[i])
                    .size(BG_WIDTH - PADDING * 2, 20)
                    .build()
            ).active = canAfford;
        }
    }

    private boolean canAfford(Specialization spec) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.getData(KRPAttachments.PLAYER_DATA).canAffordSpecialization(path, spec.getId());
    }

    private void chooseSpecialization(String specId) {
        PacketDistributor.sendToServer(new ChooseSpecializationPacket(specId));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21: renderBackground блюрит мир — размытие просвечивает сквозь панель
        // под текстом. Для оверлея затемняем простой заливкой без блюра (как PathScreen).
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - calculatedHeight) / 2;

        // Фон и рамка
        graphics.fill(x, y, x + BG_WIDTH, y + calculatedHeight, 0xCC1A1A1A);
        graphics.fill(x, y, x + BG_WIDTH, y + 1, 0xFFAA8855);
        graphics.fill(x, y + calculatedHeight - 1, x + BG_WIDTH, y + calculatedHeight, 0xFFAA8855);
        graphics.fill(x, y, x + 1, y + calculatedHeight, 0xFFAA8855);
        graphics.fill(x + BG_WIDTH - 1, y, x + BG_WIDTH, y + calculatedHeight, 0xFFAA8855);

        // Заголовок
        String pathName = XPSystem.getPathName(path);
        graphics.drawCenteredString(this.font,
                "§6Путь «" + pathName + "»",
                this.width / 2, y + 10, 0xFFFFFF);

        // Свободные очки
        var player = Minecraft.getInstance().player;
        if (player != null) {
            PlayerData data = player.getData(KRPAttachments.PLAYER_DATA);
            int available = data.getLevel(path) - data.getTotalSpentInPath(path);
            graphics.drawCenteredString(this.font,
                    "§7Свободных очков: §f" + available,
                    this.width / 2, y + 22, 0xAAAAAA);
        }

        // Описания под кнопками
        for (int i = 0; i < specs.size(); i++) {
            Specialization spec = specs.get(i);
            int descY = y + buttonYPositions[i] + 22; // сразу под кнопкой
            int currentLevel = getSpecLevel(spec.getId());
            String lvlText = currentLevel > 0 ? " (ур. " + currentLevel + ")" : "";
            String fullDesc = spec.getDescription() + lvlText;

            var lines = this.font.split(
                    net.minecraft.network.chat.Component.literal(fullDesc),
                    BG_WIDTH - PADDING * 2
            );
            for (int l = 0; l < lines.size(); l++) {
                graphics.drawString(this.font, lines.get(l),
                        x + PADDING, descY + l * 10, 0xFF888888);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int getSpecLevel(String specId) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        return player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(specId);
    }

    // Глушим ванильный блюр и фон-текстуру меню (см. PathScreen): блюр-шейдер из
    // renderBackground (его зовёт Screen.render после нашего рендера) замыливал текст.
    @Override
    protected void renderBlurredBackground(float partialTick) {}

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {}

    @Override
    public boolean isPauseScreen() { return false; }
}