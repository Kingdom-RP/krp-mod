package com.kingdomrp.core.client.screen.widget;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.PlayerSkin;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Голова скина игрока (лицо + шляпа). Онлайн-игроки берутся из списка соединения
 * (мгновенно, верный скин), офлайн — резолвятся асинхронно через сессию Mojang
 * и кэшируются. До резолва рисуется дефолтный скин.
 */
public class PlayerHeadWidget extends AbstractSpruceWidget {

    private static final Map<UUID, PlayerSkin> CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();

    private final GameProfile profile;

    public PlayerHeadWidget(Position position, int size, UUID uuid, String name) {
        super(position);
        this.width = size;
        this.height = size;
        this.profile = new GameProfile(uuid, name);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        PlayerSkin skin = resolve();
        PlayerFaceRenderer.draw(g, skin.texture(), getX(), getY(), this.width);
    }

    private PlayerSkin resolve() {
        Minecraft mc = Minecraft.getInstance();
        UUID id = profile.getId();

        // Онлайн в текущем мире — верный скин без сети.
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(id);
            if (info != null) return info.getSkin();
        }
        PlayerSkin cached = CACHE.get(id);
        if (cached != null) return cached;

        // Офлайн — тянем профиль с текстурами один раз, затем грузим скин.
        if (LOADING.add(id)) {
            CompletableFuture
                    .supplyAsync(() -> mc.getMinecraftSessionService().fetchProfile(id, false),
                            Util.backgroundExecutor())
                    .thenAcceptAsync(result -> {
                        GameProfile full = result != null ? result.profile() : profile;
                        mc.getSkinManager().getOrLoad(full)
                                .thenAccept(s -> CACHE.put(id, s));
                    }, mc);
        }
        return mc.getSkinManager().getInsecureSkin(profile);   // дефолт до резолва
    }
}
