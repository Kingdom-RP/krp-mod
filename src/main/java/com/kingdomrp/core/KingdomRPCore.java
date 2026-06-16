package com.kingdomrp.core;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.network.NetworkHandler;
import com.kingdomrp.core.registry.KRPEffects;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KingdomRPCore.MODID)
public class KingdomRPCore {

    public static final String MODID = "kingdomrpcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KingdomRPCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::commonSetup);
        KRPEffects.register(modEventBus);

        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                KRPConfig.SPEC,
                "kingdomrpcore-server.toml"
        );

        LOGGER.info("KingdomRP Core loaded!");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
