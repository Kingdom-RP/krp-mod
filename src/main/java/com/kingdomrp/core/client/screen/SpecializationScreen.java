package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.network.ChooseSpecializationPacket;
import com.kingdomrp.core.data.type.Specialization;
import com.kingdomrp.core.data.type.SpecializationRegistry;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.thinkingstudio.obsidianui.screen.SpruceScreen;

import java.util.List;

public class SpecializationScreen extends SpruceScreen {

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
            String desc = specs.get(i).description();
            var lines = this.font.split(
                    net.minecraft.network.chat.Component.literal(desc),
                    BG_WIDTH - PADDING * 2
            );
            currentY += lines.size() * 10 + 8; // строки + отступ снизу
        }

        calculatedHeight = Math.max(BG_HEIGHT, currentY + 34);   // +место под кнопку «Назад»
    }

    private final Path path;
    private final List<Specialization> specs;
    private Button[] specButtons = new Button[0];

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

        specButtons = new Button[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            Specialization spec = specs.get(i);
            Button b = this.addRenderableWidget(Button.builder(
                            Component.literal(labelFor(spec)),
                            btn -> chooseSpecialization(spec.id()))
                    .pos(x + PADDING, y + buttonYPositions[i])
                    .size(BG_WIDTH - PADDING * 2, 20)
                    .build());
            b.active = canAfford(spec);
            specButtons[i] = b;
        }

        // Кнопка «Назад» — вернуться к экрану путей (хаб, вкладка «Прокачка»).
        this.addRenderableWidget(Button.builder(
                        Component.literal("§e← Назад"),
                        btn -> Minecraft.getInstance().setScreen(new KingdomHubScreen()))
                .pos(x + PADDING, y + calculatedHeight - 26)
                .size(BG_WIDTH - PADDING * 2, 20)
                .build());
    }

    private String labelFor(Specialization spec) {
        return getSpecLevel(spec.id()) >= PlayerData.MAX_SPEC_LEVEL
                ? spec.name() + " §a[МАКС]" : spec.name();
    }

    /** Экран не закрывается на выбор — обновляем доступность/подписи после синка данных. */
    @Override
    public void tick() {
        for (int i = 0; i < specButtons.length && i < specs.size(); i++) {
            Specialization spec = specs.get(i);
            if (specButtons[i] != null) {
                specButtons[i].active = canAfford(spec);
                specButtons[i].setMessage(Component.literal(labelFor(spec)));
            }
        }
    }

    private boolean canAfford(Specialization spec) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.getData(KRPAttachments.PLAYER_DATA).canAffordSpecialization(path, spec.id());
    }

    private void chooseSpecialization(String specId) {
        // Экран НЕ закрываем — при нескольких свободных очках можно качать подряд.
        // Доступность кнопок обновится в tick() после серверного синка данных.
        PacketDistributor.sendToServer(new ChooseSpecializationPacket(specId));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Оверлей поверх игры без полноэкранного затемнения — единый вид с хабом.
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
            int currentLevel = getSpecLevel(spec.id());
            String lvlText;
            if (currentLevel >= PlayerData.MAX_SPEC_LEVEL) {
                lvlText = " §a(ур. " + currentLevel + " — макс. уровень)";
            } else if (currentLevel > 0) {
                lvlText = " (ур. " + currentLevel + ")";
            } else {
                lvlText = "";
            }
            String fullDesc = spec.description() + lvlText;

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

    // Глушим ванильный блюр/фон-текстуру меню (иначе ручной бокс+текст уезжают за размытие).
    // Полноэкранного затемнения НЕ добавляем — единый вид с хабом (игра видна за оверлеем).
    @Override
    protected void renderBlurredBackground(float partialTick) {}

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {}

    @Override
    public boolean isPauseScreen() { return false; }
}