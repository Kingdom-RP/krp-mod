package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.map.tier.BlockTierMap;
import com.kingdomrp.core.data.map.xp.BlockXPMap;
import com.kingdomrp.core.data.map.xp.FoodCookMap;
import com.kingdomrp.core.data.map.tier.FoodTierMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.map.tier.PlantTierMap;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Мягкая интеграция Farmer's Delight (без хард-зависимости). Контент FD
 * добавляется в существующие карты ограничений ПО ID через реестры
 * ({@code addById} — no-op, если предмета/блока нет). Активируется только если
 * мод загружен. Гейт готовки в Cooking Pot — отдельный опциональный мизин
 * ({@code compat.mixin.CookingPotMealSlotMixin} + {@link FDMixinPlugin}).
 * <p>
 * Тиры подобраны по аналогии с ванилью (культуры — по сложности, еда — по
 * сытости/составу). Всё в одном месте — правится здесь.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class FarmersDelightCompat {

    public static final String MODID = "farmersdelight";

    private FarmersDelightCompat() {}

    private static String id(String path) {
        return MODID + ":" + path;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(FarmersDelightCompat::register);
    }

    private static void register() {
        registerCrops();
        registerFoods();
        registerTools();
    }

    // ============================ КУЛЬТУРЫ (Фермер) ============================
    // Посадка (PlantTierMap) + гейт добычи (BlockTierMap) + XP за сбор (BlockXPMap).
    // Дикие (wild_*) НЕ гейтим — это источник стартовых семян.

    private static void registerCrops() {
        crop("cabbages", 1, 3f);
        crop("onions", 1, 3f);
        crop("tomatoes", 2, 3f);          // растущая стадия (если есть)
        crop("budding_tomatoes", 2, 3f);  // спелая стадия с плодами
        crop("rice", 2, 3f);
        harvest("rice_panicles", 2, 3f); // верхняя часть риса — только гейт сбора/XP
        crop("brown_mushroom_colony", 3, 4f);
        crop("red_mushroom_colony", 3, 4f);
    }

    /** Полный набор для растущей культуры: посадка (без XP) + добыча + XP за сбор. */
    private static void crop(String path, int level, float xp) {
        PlantTierMap.addById(id(path), level, true);
        BlockTierMap.addById(id(path), Spec.FARMER, level);
        BlockXPMap.addById(id(path), Path.HARVEST, xp);
    }

    /** Только гейт добычи + XP (без посадки). */
    private static void harvest(String path, int level, float xp) {
        BlockTierMap.addById(id(path), Spec.FARMER, level);
        BlockXPMap.addById(id(path), Path.HARVEST, xp);
    }

    // ============================== ЕДА (Повар) ===============================
    // Гейт ПРОИЗВОДСТВА еды по уровню Повара (FoodTierMap). Покрывает крафт в
    // верстаке (SlotMixin) и готовку в Cooking Pot (CookingPotMealSlotMixin).
    // Промежуточные заготовки (тесто, паста, фарш, нарезка) НЕ гейтим — они
    // делаются на доске/верстаке и сами по себе не «еда».

    private static void registerFoods() {
        // Тиры по «ценности» = nutrition × (1 + 2·saturation) и сложности крафта
        // (1 — лёгкая малопитательная еда … 10 — очень питательная). Напитки
        // (cider/milk/cocoa), сырьё-полуфабрикаты, дробные слайсы и корм животным
        // (dog_food/horse_feed) НЕ гейтим. Значения легко правятся здесь.

        // Тир 1 — печеньки, фруктовый лёд (ценность ~2–4)
        cook(1, "honey_cookie", "sweet_berry_cookie", "melon_popsicle");

        // Тир 2 — яичница (~7)
        cook(2, "fried_egg");

        // Тир 3 — варёный рис, простые салаты/голубцы (~9–11)
        cook(3, "cooked_rice", "nether_salad", "cabbage_rolls");

        // Тир 4 — салаты, роллы, кастард (~13–16)
        cook(4, "fruit_salad", "mixed_salad", "glow_berry_custard",
                "salmon_roll", "cod_roll");

        // Тир 5 — костный бульон, яичный сэндвич, пельмени, яблочный пирог (~19–21)
        cook(5, "bone_broth", "egg_sandwich", "dumplings", "apple_pie");

        // Тир 6 — бекон с яйцом, рататуй, шашлычок, фаршированная картошка, пироги (~22–24)
        cook(6, "bacon_and_eggs", "ratatouille", "barbecue_stick", "stuffed_potato",
                "chocolate_pie", "sweet_berry_cheesecake");

        // Тир 7 — сэндвичи/врапы, копчёный окорок (~26)
        cook(7, "chicken_sandwich", "bacon_sandwich", "mutton_wrap", "smoked_ham");

        // Тир 8 — бургер, супы/рагу, рис, паста, стейк (~29–31)
        cook(8, "hamburger", "beef_stew", "vegetable_soup", "fish_stew",
                "onion_soup", "chicken_soup", "fried_rice", "mushroom_rice",
                "steak_and_potatoes", "pasta_with_meatballs", "pasta_with_mutton_chop");

        // Тир 9 — сытные «феасты» и наваристые блюда (~35)
        cook(9, "pumpkin_soup", "baked_cod_stew", "noodle_soup", "roasted_mutton_chops",
                "vegetable_noodles", "squid_ink_pasta", "grilled_salmon", "roast_chicken",
                "stuffed_pumpkin", "honey_glazed_ham", "shepherds_pie", "gleaming_salad");

        // Тир 10 — рулет из ламинарии (рекордное насыщение, ценность ~70)
        cook(10, "kelp_roll");
    }

    private static void cook(int level, String... paths) {
        for (String p : paths) {
            FoodTierMap.addById(id(p), Spec.COOK, level);          // гейт производства по уровню
            FoodCookMap.addById(id(p), FoodCookMap.xpForTier(level)); // XP за готовку (как ваниль)
        }
    }

    // ============================== НОЖИ (Кузнец) =============================
    // Крафт ножей гейтится как инструменты соответствующего материала.

    private static void registerTools() {
        ItemCraftTierMap.addById(id("golden_knife"), new SpecRequirement(Spec.BLACKSMITH, 2));
        ItemCraftTierMap.addById(id("iron_knife"), new SpecRequirement(Spec.BLACKSMITH, 3));
        ItemCraftTierMap.addById(id("diamond_knife"), new SpecRequirement(Spec.BLACKSMITH, 5));
        ItemCraftTierMap.addById(id("netherite_knife"), new SpecRequirement(Spec.BLACKSMITH, 7));
        // flint_knife — стартовый, без гейта.
    }
}
