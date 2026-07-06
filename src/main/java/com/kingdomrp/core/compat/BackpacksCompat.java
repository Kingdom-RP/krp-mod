package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Мягкая интеграция мода Backpacks (без хард-зависимости). Крафт рюкзаков
 * привязан к уровню Мастерового (натуральные материалы — кожа/нить):
 * обычный рюкзак — ур.3, улучшенный — ур.5. Регистрация по ID через реестр
 * ({@code addById} — no-op, если предмета нет). Активна только если мод загружен.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class BackpacksCompat {

    public static final String MODID = "backpacks";

    private BackpacksCompat() {}

    private static String id(String path) {
        return MODID + ":" + path;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(BackpacksCompat::register);
    }

    private static void register() {
        ItemCraftTierMap.addById(id("backpack"), new SpecRequirement(Spec.CRAFTSMAN, 3));
        ItemCraftTierMap.addById(id("large_backpack"), new SpecRequirement(Spec.CRAFTSMAN, 5));
    }
}
