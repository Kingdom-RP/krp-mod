package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
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
