package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KingdomRPCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class KeyBindings {

    public static final KeyMapping OPEN_PATHS = new KeyMapping(
            "key.kingdomrpcore.open_paths",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.kingdomrpcore"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PATHS);
    }
}