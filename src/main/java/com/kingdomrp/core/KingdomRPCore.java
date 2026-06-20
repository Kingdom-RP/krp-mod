package com.kingdomrp.core;

import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.network.NetworkHandler;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.registry.KRPEffects;
import com.kingdomrp.core.system.ModWhitelist;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(KingdomRPCore.MODID)
public class KingdomRPCore {

    public static final String MODID = "kingdomrpcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    // NeoForge 1.21: конструктор @Mod-класса получает шину мода и ModContainer.
    public KingdomRPCore(IEventBus modEventBus, ModContainer modContainer) {
        KRPAttachments.register(modEventBus);
        KRPEffects.register(modEventBus);
        // Регистрация сетевых пакетов — событие RegisterPayloadHandlersEvent на шине мода.
        modEventBus.addListener(NetworkHandler::register);
        // Проверка модов клиента по белому списку — задача config-фазы (шина мода).
        modEventBus.addListener(ModWhitelist::onRegisterConfigurationTasks);

        modContainer.registerConfig(
                ModConfig.Type.SERVER, KRPConfig.SPEC, "kingdomrpcore-server.toml");

        LOGGER.info("KingdomRP Core loaded!");
    }
}
