package com.kingdomrp.core.client.screen.widget;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;

import java.util.UUID;

/** Голова скина игрока (лицо + шляпа). */
public class PlayerHeadWidget extends AbstractSpruceWidget {

    private final GameProfile profile;

    public PlayerHeadWidget(Position position, int size, UUID uuid, String name) {
        super(position);
        this.width = size;
        this.height = size;
        this.profile = new GameProfile(uuid, name);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        var skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
        PlayerFaceRenderer.draw(g, skin.texture(), getX(), getY(), this.width);
    }
}
