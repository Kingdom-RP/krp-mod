package com.kingdomrp.core.client.screen;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.client.ClientKingdomData;
import com.kingdomrp.core.client.KingdomSyncListener;
import com.kingdomrp.core.client.screen.widget.InviteSuggestWidget;
import com.kingdomrp.core.client.screen.widget.PathPanelWidget;
import com.kingdomrp.core.client.screen.widget.PlayerHeadWidget;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.network.InvitePlayerPacket;
import com.kingdomrp.core.network.KickMemberPacket;
import com.kingdomrp.core.network.SetKingdomColorPacket;
import com.kingdomrp.core.network.SyncKingdomInfoPacket;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.SpruceTexts;
import org.thinkingstudio.obsidianui.background.SimpleColorBackground;
import org.thinkingstudio.obsidianui.border.SimpleBorder;
import org.thinkingstudio.obsidianui.screen.SpruceScreen;
import org.thinkingstudio.obsidianui.widget.SpruceButtonWidget;
import org.thinkingstudio.obsidianui.widget.SpruceLabelWidget;
import org.thinkingstudio.obsidianui.widget.container.SpruceContainerWidget;
import org.thinkingstudio.obsidianui.widget.container.tabbed.SpruceTabbedWidget;
import org.thinkingstudio.obsidianui.widget.text.SpruceTextFieldWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Хаб-меню (K): «Прокачка», «Королевство» (членам) и встроенное «Управление
 * королевством» (королю) с подвкладками Жители/Управление.
 */
public class KingdomHubScreen extends SpruceScreen implements KingdomSyncListener {

    private SpruceTabbedWidget tabbed;
    private String pendingTabTitle;       // сохранение выбранной боковой вкладки при rebuild

    // Состояние встроенного управления (переживает rebuild — поля инстанса).
    private int manageSubTab = 0;         // 0 = жители, 1 = управление
    private String inviteText = "";
    private SpruceContainerWidget residentsPanel, controlPanel;
    private com.kingdomrp.core.client.screen.widget.FlatTabWidget btnResidents, btnControl;

    public KingdomHubScreen() {
        super(Component.translatable("kingdomrp.menu.title"));
    }

    @Override
    public void onKingdomSync() {
        if (tabbed != null && tabbed.getList().getCurrentTab() != null)
            pendingTabTitle = tabbed.getList().getCurrentTab().getTitle().getString();
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        this.tabbed = new SpruceTabbedWidget(Position.of(this, 0, 4), this.width, this.height - 35 - 4, this.title);
        this.tabbed.addTabEntry(Component.translatable("kingdomrp.menu.tab.progress"), null, this::buildProgressTab);
        if (ClientKingdomData.inKingdom()) {
            this.tabbed.addTabEntry(Component.translatable("kingdomrp.menu.tab.kingdom"), null, this::buildKingdomTab);
            if (ClientKingdomData.get().isKing())
                this.tabbed.addTabEntry(Component.translatable("kingdomrp.menu.tab.manage"), null, this::buildManageTab);
        }
        this.addRenderableWidget(this.tabbed);
        reselectTab();

        this.addRenderableWidget(new SpruceButtonWidget(
                Position.of(this, this.width / 2 - 75, this.height - 29), 150, 20,
                SpruceTexts.GUI_DONE, btn -> this.onClose()).asVanilla());
    }

    private void reselectTab() {
        if (pendingTabTitle == null) return;
        for (var e : tabbed.getList().children()) {
            if (e instanceof SpruceTabbedWidget.TabEntry t && t.getTitle().getString().equals(pendingTabTitle)) {
                tabbed.getList().setSelected(t);
                break;
            }
        }
        pendingTabTitle = null;
    }

    private int sideOffset(int contentWidth) { return this.width - contentWidth; }

    private SpruceContainerWidget styledPanel(int x, int y, int w, int h) {
        var p = new SpruceContainerWidget(Position.of(x, y), w, h);
        p.setBackground(new SimpleColorBackground(0xCC1A1A1A));
        p.setBorder(new SimpleBorder(1, 0xFFAA8855));
        return p;
    }

    // ===== Прокачка =====

    private SpruceContainerWidget buildProgressTab(int width, int height) {
        var outer = new SpruceContainerWidget(Position.origin(), width, height);
        var player = Minecraft.getInstance().player;
        List<Path> withPoints = new ArrayList<>();
        if (player != null) {
            PlayerData data = player.getData(KRPAttachments.PLAYER_DATA);
            for (Path path : Path.values())
                if (data.hasAvailablePoints(path)) withPoints.add(path);
        }

        int panelW = 290, barsTop = 30, barsH = Path.values().length * 36;
        int btnTop = barsTop + barsH + 6;
        int panelH = btnTop + withPoints.size() * 22 + 10;
        int panelX = this.width / 2 - panelW / 2 - sideOffset(width);
        var panel = styledPanel(panelX, Math.max(0, (height - panelH) / 2), panelW, panelH);

        panel.addChildren((w, h, adder) -> {
            adder.accept(new SpruceLabelWidget(Position.of(0, 10),
                    Component.literal("§6⚔ Пути развития ⚔"), w, true));
            adder.accept(new PathPanelWidget(Position.of(18, barsTop), w - 36, barsH));
            int y = btnTop;
            for (Path path : withPoints) {
                adder.accept(new SpruceButtonWidget(Position.of(15, y), w - 30, 20,
                        Component.literal("§6► " + XPSystem.getPathName(path) + " ◄"),
                        btn -> Minecraft.getInstance().setScreen(new SpecializationScreen(path))));
                y += 22;
            }
        });
        outer.addChild(panel);
        return outer;
    }

    // ===== Королевство (инфо) =====

    private SpruceContainerWidget buildKingdomTab(int width, int height) {
        var outer = new SpruceContainerWidget(Position.origin(), width, height);
        SyncKingdomInfoPacket info = ClientKingdomData.get();

        java.util.List<Component> buffs = buffLines(info);
        int panelW = 280;
        int barsY = 56, buffsY = barsY + 3 * 24 + 6;
        int buffsEnd = buffsY + 14 + buffs.size() * 12;
        int resY = buffsEnd + 6;
        boolean isKing = ClientKingdomData.get().isKing();
        int membersEnd = resY + 16 + info.members().size() * 20;
        int panelH = membersEnd + 8 + (isKing ? 0 : 26);
        int panelX = this.width / 2 - panelW / 2 - sideOffset(width);
        var panel = styledPanel(panelX, Math.max(0, (height - panelH) / 2), panelW, panelH);

        panel.addChildren((w, h, adder) -> {
            adder.accept(new SpruceLabelWidget(Position.of(0, 10),
                    Component.literal(info.name()).withStyle(
                            Style.EMPTY.withColor(TextColor.fromRgb(info.color())).withBold(true)), w, true));
            adder.accept(new SpruceLabelWidget(Position.of(0, 28),
                    Component.translatable("kingdomrp.menu.king", info.kingName()).withStyle(ChatFormatting.WHITE), w, true));
            adder.accept(new SpruceLabelWidget(Position.of(0, 42),
                    Component.translatable("kingdomrp.menu.claims", info.claims()).withStyle(ChatFormatting.GRAY), w, true));

            float max = com.kingdomrp.core.kingdom.upkeep.Characteristic.MAX;
            int bw = w - 24;
            adder.accept(new com.kingdomrp.core.client.screen.widget.ResourceBarWidget(Position.of(12, barsY), bw,
                    Component.translatable("kingdomrp.upkeep.food"), info.food(), max, 0xFF2ECC71));
            adder.accept(new com.kingdomrp.core.client.screen.widget.ResourceBarWidget(Position.of(12, barsY + 24), bw,
                    Component.translatable("kingdomrp.upkeep.materials"), info.materials(), max, 0xFFB5651D));
            adder.accept(new com.kingdomrp.core.client.screen.widget.ResourceBarWidget(Position.of(12, barsY + 48), bw,
                    Component.translatable("kingdomrp.upkeep.prosperity"), info.prosperity(), max, 0xFF3498DB));

            // Активные баффы/дебаффы.
            adder.accept(new SpruceLabelWidget(Position.of(0, buffsY),
                    Component.translatable("kingdomrp.buff.header").withStyle(ChatFormatting.WHITE), w, true));
            for (int i = 0; i < buffs.size(); i++)
                adder.accept(new SpruceLabelWidget(Position.of(0, buffsY + 14 + i * 12), buffs.get(i), w, true));

            // Список жителей (голова + ник).
            adder.accept(new SpruceLabelWidget(Position.of(0, resY),
                    Component.translatable("kingdomrp.menu.residents_header").withStyle(ChatFormatting.WHITE), w, true));
            for (int i = 0; i < info.members().size(); i++) {
                int ry = resY + 16 + i * 20;
                String name = info.members().get(i);
                boolean king = name.equals(info.kingName());
                adder.accept(new PlayerHeadWidget(Position.of(w / 2 - 70, ry), 16, info.memberIds().get(i), name));
                adder.accept(new SpruceLabelWidget(Position.of(w / 2 - 48, ry + 4),
                        Component.literal(king ? "♔ " + name : name)
                                .withStyle(king ? ChatFormatting.GOLD : ChatFormatting.WHITE), 140, false));
            }

            // Кнопка выхода — только не-королю (король уходит через роспуск).
            if (!isKing) {
                adder.accept(new SpruceButtonWidget(Position.of(w / 2 - 75, membersEnd + 4), 150, 20,
                        Component.translatable("kingdomrp.menu.leave").withStyle(ChatFormatting.RED),
                        btn -> {
                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                    new com.kingdomrp.core.network.LeaveKingdomPacket());
                            this.onClose();
                        }));
            }
        });
        outer.addChild(panel);
        return outer;
    }

    private static int stepOf(float value) {
        return Math.max(-5, Math.min(5, Math.round((value - 500f) / 100f)));
    }

    /** Строки активных баффов/дебаффов (зелёные/красные). */
    private java.util.List<Component> buffLines(SyncKingdomInfoPacket info) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        int f = stepOf(info.food()), m = stepOf(info.materials()), p = stepOf(info.prosperity());
        if (f != 0) out.add(sign(f, String.format("%s: %+d ХП, голод %+d%%",
                "Продовольствие", f, -5 * f)));
        if (m != 0) out.add(sign(m, String.format("%s: скорость добычи %+d%%", "Материалы", 5 * m)));
        if (p != 0) out.add(sign(p, String.format("%s: опыт %+d%%, удача %+.1f", "Довольствие", 5 * p, 0.5 * p)));
        if (out.isEmpty()) out.add(Component.translatable("kingdomrp.buff.none").withStyle(ChatFormatting.GRAY));
        return out;
    }

    private static Component sign(int step, String text) {
        return Component.literal(text).withStyle(step > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    // ===== Управление (встроенное, подвкладки) =====

    private SpruceContainerWidget buildManageTab(int width, int height) {
        var outer = new SpruceContainerWidget(Position.origin(), width, height);
        SyncKingdomInfoPacket info = ClientKingdomData.get();

        int panelW = 300, tabW = 110, tabsY = 6, panelY = tabsY + 20;
        int panelX = this.width / 2 - panelW / 2 - sideOffset(width);
        int panelH = height - panelY - 6;

        residentsPanel = styledPanel(panelX, panelY, panelW, panelH);
        controlPanel = styledPanel(panelX, panelY, panelW, panelH);
        fillResidents(residentsPanel, info);
        fillControl(controlPanel, info);

        // Плоские подвкладки в стиле ObsidianUI — прилегают к панели (низ вкладки = верх панели).
        btnResidents = new com.kingdomrp.core.client.screen.widget.FlatTabWidget(
                Position.of(panelX, tabsY), tabW, 21,
                Component.translatable("kingdomrp.manage.tab.residents"),
                () -> { manageSubTab = 0; applyManageSub(); });
        btnControl = new com.kingdomrp.core.client.screen.widget.FlatTabWidget(
                Position.of(panelX + tabW + 2, tabsY), tabW, 21,
                Component.translatable("kingdomrp.manage.tab.control"),
                () -> { manageSubTab = 1; applyManageSub(); });

        outer.addChild(residentsPanel);
        outer.addChild(controlPanel);
        outer.addChild(btnResidents);
        outer.addChild(btnControl);
        applyManageSub();
        return outer;
    }

    private void applyManageSub() {
        residentsPanel.setVisible(manageSubTab == 0);
        controlPanel.setVisible(manageSubTab == 1);
        btnResidents.setSelected(manageSubTab == 0);
        btnControl.setSelected(manageSubTab == 1);
    }

    private void fillResidents(SpruceContainerWidget panel, SyncKingdomInfoPacket info) {
        panel.addChildren((w, h, adder) -> {
            for (int i = 0; i < info.members().size(); i++) {
                int y = 10 + i * 24;
                String name = info.members().get(i);
                var uuid = info.memberIds().get(i);
                boolean isKing = name.equals(info.kingName());
                adder.accept(new PlayerHeadWidget(Position.of(12, y), 18, uuid, name));
                adder.accept(new SpruceLabelWidget(Position.of(38, y + 5),
                        Component.literal(isKing ? "♔ " + name : name)
                                .withStyle(isKing ? ChatFormatting.GOLD : ChatFormatting.WHITE), 160, false));
                if (!isKing) {
                    adder.accept(new SpruceButtonWidget(Position.of(w - 82, y), 72, 18,
                            Component.translatable("kingdomrp.manage.kick"),
                            btn -> PacketDistributor.sendToServer(new KickMemberPacket(uuid))));
                }
            }
        });
    }

    private void fillControl(SpruceContainerWidget panel, SyncKingdomInfoPacket info) {
        var field = new SpruceTextFieldWidget(Position.of(20, 14), 180, 18,
                Component.translatable("kingdomrp.manage.invite_hint"));
        field.setText(inviteText);
        field.setChangedListener(s -> this.inviteText = s);

        panel.addChildren((w, h, adder) -> {
            adder.accept(field);
            adder.accept(new SpruceButtonWidget(Position.of(210, 14), 70, 18,
                    Component.translatable("kingdomrp.manage.invite"), btn -> {
                        String n = inviteText.trim();
                        if (!n.isEmpty()) {
                            PacketDistributor.sendToServer(new InvitePlayerPacket(n));
                            this.inviteText = "";
                            field.setText("");
                        }
                    }));
            adder.accept(new InviteSuggestWidget(Position.of(20, 36), 180, field,
                    () -> info.members(), name -> { this.inviteText = name; field.setText(name); }));
            adder.accept(new SpruceButtonWidget(Position.of(20, 112), 260, 20,
                    Component.translatable("kingdomrp.menu.change_color"),
                    btn -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                            this, Component.translatable("kingdomrp.color.pick_title"), info.color(),
                            color -> PacketDistributor.sendToServer(new SetKingdomColorPacket(color))))));
            adder.accept(new SpruceLabelWidget(Position.of(0, 140),
                    Component.translatable("kingdomrp.manage.pending").withStyle(ChatFormatting.WHITE), w, true));
            for (int i = 0; i < info.pendingInvites().size(); i++) {
                adder.accept(new SpruceLabelWidget(Position.of(0, 156 + i * 14),
                        Component.literal("• " + info.pendingInvites().get(i)).withStyle(ChatFormatting.GRAY), w, true));
            }
        });
    }

    /** Заголовок не рисуем — он уже над боковым списком вкладок (без дубля). */
    @Override
    public void renderTitle(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
