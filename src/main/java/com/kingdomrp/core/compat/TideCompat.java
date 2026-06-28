package com.kingdomrp.core.compat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.FishingXPMap;
import com.kingdomrp.core.data.FoodCookMap;
import com.kingdomrp.core.data.FoodTierMap;
import com.kingdomrp.core.data.ItemCraftTierMap;
import com.kingdomrp.core.data.ItemUseTierMap;
import com.kingdomrp.core.data.Spec;
import com.kingdomrp.core.data.SpecRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Мягкая интеграция мода Tide (рыбалка) — без хард-зависимости. Снасти Tide
 * привязаны к уровню Рыбака ({@link Spec#FISHERMAN}):
 * <ul>
 *   <li>КРАФТ (наживки/крючки/лески/удочки/стол) — {@link ItemCraftTierMap} (гейт
 *       изъятия результата), чем «значимее» предмет, тем выше уровень.</li>
 *   <li>ИСПОЛЬЗОВАНИЕ удочек (каст) — {@link ItemUseTierMap} (отмена ПКМ): мощную
 *       удочку нельзя забросить ниже своего уровня.</li>
 *   <li>XP за улов по редкости вида — Tide хранит рыбу в датапаке
 *       {@code data/<ns>/fishing/fish/**.json} (поле {@code journal_profile.rarity},
 *       {@code fish} = ID предмета). На перезагрузке датапаков читаем те же файлы
 *       и строим карту «рыба → XP» по редкости (см. {@link #fishXP(Item)} и
 *       {@link FishingXPMap}). Виды не перечисляются вручную.</li>
 *   <li>ГОТОВКА жареной рыбы Tide — гейт по уровню Повара ({@link FoodTierMap}) +
 *       XP ({@link FoodCookMap}), как ванильная/FD еда.</li>
 * </ul>
 * Бобберы — косметика, не гейтятся. Ускоренный клёв — {@link TideMixinPlugin}.
 * Регистрация по ID через реестр (no-op, если предмета нет). Активна только если
 * мод загружен.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class TideCompat {

    public static final String MODID = "tide";
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Предмет рыбы → XP. Перестраивается на каждой перезагрузке датапаков. */
    private static volatile Map<Item, Float> FISH_XP = Map.of();

    private TideCompat() {}

    private static String id(String path) {
        return MODID + ":" + path;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(TideCompat::register);
    }

    private static void register() {
        registerConsumables();
        registerRods();
        registerCooking();
    }

    /** Крафт наживок/крючков/лесок/стола — по значимости. */
    private static void registerConsumables() {
        // Angling Table — нужна рано для кастомизации снастей
        craft("angling_table", 1);

        // Наживки
        craft("bait", 1);
        craft("lucky_bait", 3);
        craft("magnetic_bait", 3);
        craft("incandescent_bait", 3);
        craft("abyss_bait", 7);

        // Крючки
        craft("fishing_hook", 1);
        craft("fiery_hook", 2);
        craft("permafrost_hook", 2);
        craft("twilight_hook", 4);
        craft("lavaproof_hook", 6);
        craft("void_hook", 8);

        // Лески
        craft("fishing_line", 1);
        craft("copper_line", 2);
        craft("iron_line", 3);
        craft("golden_line", 4);
        craft("diamond_line", 5);
    }

    /**
     * Удочки: гейт И крафта, И использования одним уровнем. Стартовые
     * (vanilla/stone/iron) — ур.0, не гейтятся. «Спец»-удочки из лута гейт крафта
     * не имеют (нет рецепта) — гейт использования всё равно действует.
     */
    private static void registerRods() {
        rod("stone_fishing_rod", 1);
        rod("iron_fishing_rod", 2);
        rod("golden_fishing_rod", 3);
        rod("crystal_fishing_rod", 3);
        rod("diamond_fishing_rod", 4);
        rod("netherite_fishing_rod", 6);
        rod("echo_fishing_rod", 5);
        rod("prismarine_fishing_rod", 6);
        rod("midas_fishing_rod", 10);
        rod("village_fishing_rod", 7);
        rod("blazing_fishing_rod", 7);
        rod("honeycomb_fishing_rod", 8);
        rod("sunflower_fishing_rod", 9);
    }

    /**
     * Гейт ПРОИЗВОДСТВА жареной рыбы Tide по уровню Повара (как ваниль/FD): печь/
     * коптильня/костёр — {@link FoodTierMap} (гейт результата) + XP {@link FoodCookMap}.
     * Тиры по пищевой ценности (голод + насыщение): мелкая=1, обычная=2, тунец=4,
     * крупная=5; ломтик=1. Сырьё ({@code #tide:cookable/*}) — вход, гейтим результат.
     */
    private static void registerCooking() {
        cook("small_cooked_fish", 1);
        cook("cooked_fish", 2);
        cook("cooked_fish_slice", 1);
        cook("grilled_tuna", 4);
        cook("large_cooked_fish", 5);
    }

    private static void craft(String name, int level) {
        ItemCraftTierMap.addById(id(name), new SpecRequirement(Spec.FISHERMAN, level));
    }

    private static void rod(String name, int level) {
        SpecRequirement req = new SpecRequirement(Spec.FISHERMAN, level);
        ItemCraftTierMap.addById(id(name), req);
        ItemUseTierMap.addById(id(name), req);
    }

    private static void cook(String name, int tier) {
        FoodTierMap.addById(id(name), Spec.COOK, tier);
        FoodCookMap.addById(id(name), FoodCookMap.xpForTier(tier));
    }

    // ===================== XP ЗА УЛОВ ПО РЕДКОСТИ ВИДА =======================
    // Tide — движок рыбалки: пойманные предметы принадлежат другим модам, а
    // редкость вида лежит в датапаке data/<ns>/fishing/fish/**.json. Читаем те же
    // файлы на перезагрузке датапаков и строим карту «рыба → XP». 5 редкостей Tide →
    // 5 уровней XP: common 5, uncommon 8, rare 10, very_rare 15, legendary 25.

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.addListener((ResourceManagerReloadListener) TideCompat::reloadFishXP);
    }

    private static void reloadFishXP(ResourceManager rm) {
        Map<Item, Float> map = new HashMap<>();
        var resources = rm.listResources("fishing/fish", p -> p.getPath().endsWith(".json"));
        for (var entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null || !json.has("fish")) continue;

                Item item = BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse(json.get("fish").getAsString()));
                if (item == Items.AIR) continue;

                map.put(item, xpForRarity(rarityOf(json)));
            } catch (Exception e) {
                LOGGER.warn("Tide: не удалось прочитать рыбу {}", entry.getKey(), e);
            }
        }
        FISH_XP = Map.copyOf(map);
        LOGGER.info("Tide compat: размечено {} видов рыбы по редкости", map.size());
    }

    /** Редкость из {@code journal_profile.rarity} (default — common). */
    private static String rarityOf(JsonObject json) {
        if (json.has("journal_profile")) {
            JsonObject profile = json.getAsJsonObject("journal_profile");
            if (profile.has("rarity")) return profile.get("rarity").getAsString();
        }
        return "common";
    }

    private static float xpForRarity(String rarity) {
        return switch (rarity) {
            case "uncommon" -> FishingXPMap.UNCOMMON_XP;   // 8
            case "rare" -> FishingXPMap.RARE_XP;           // 10
            case "very_rare" -> FishingXPMap.VERY_RARE_XP; // 15
            case "legendary" -> FishingXPMap.TREASURE_XP;  // 25
            default -> FishingXPMap.COMMON_XP;             // common и неизвестные = 5
        };
    }

    /** XP за рыбу Tide, либо 0 — если предмет не размеченная рыба. */
    public static float fishXP(Item item) {
        return FISH_XP.getOrDefault(item, 0f);
    }
}
