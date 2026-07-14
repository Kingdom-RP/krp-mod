package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.map.xp.FoodCookMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Мягкая интеграция Hybrid Aquatic (без хард-зависимости). Готовка рыбной еды HA
 * (печь/коптильня/костёр) даёт XP Повару, как ванильная/Tide/FD еда — результат
 * регистрируется в {@link FoodCookMap} по ID (no-op, если предмета нет).
 * <p>
 * ⚠️ modId мода — {@code hybrid_aquatic}, а namespace предметов — {@code hybrid-aquatic}
 * (через дефис). Гейт производства (FoodTierMap) НЕ ставим — только XP.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class HybridAquaticCompat {

    public static final String MODID = "hybrid_aquatic";
    private static final String NS = "hybrid-aquatic";

    private HybridAquaticCompat() {}

    private static String id(String path) {
        return NS + ":" + path;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(HybridAquaticCompat::register);
    }

    private static void register() {
        // XP за готовку по пищевой ценности (тир → XP как ваниль). Гейта нет.
        cook(1, "cooked_shrimp", "cooked_crayfish");
        cook(2, "cooked_crab", "cooked_tentacle", "cooked_fish_meat");
        cook(3, "cooked_lobster_tail", "cooked_fish_steak");
        cook(4, "cooked_lobster");
    }

    private static void cook(int tier, String... paths) {
        for (String p : paths) {
            FoodCookMap.addById(id(p), FoodCookMap.xpForTier(tier));
        }
    }
}
