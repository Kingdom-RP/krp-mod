package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class ItemCraftMap {

    private static final Map<Item, CraftEntry> MAP = new HashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // ================================================================
        // ПЛОТНИК
        // ================================================================
        // Крафт деревянных (и «не очень») изделий — всегда успех.
        // Доступ по уровням — ItemCraftTierMap (CARPENTER). Гейтинг стройки —
        // по образцу старого KRP. XP по ценности изделия (см. ниже), группировка
        // здесь по тирам доступа для сверки с ItemCraftTierMap.

        // ---------------- Тир 0 (без гейта): минимум для старта ----------------

        // Доски, палка, миска, знаки — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS,
                Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
                Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS,
                Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.STICK, Items.BOWL);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_SIGN, Items.BIRCH_SIGN, Items.SPRUCE_SIGN,
                Items.JUNGLE_SIGN, Items.ACACIA_SIGN, Items.DARK_OAK_SIGN,
                Items.MANGROVE_SIGN, Items.CHERRY_SIGN, Items.BAMBOO_SIGN,
                Items.CRIMSON_SIGN, Items.WARPED_SIGN);

        // Верстак, сундук, деревянные инструменты — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.CRAFTING_TABLE, Items.CHEST);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_PICKAXE,
                Items.WOODEN_SHOVEL, Items.WOODEN_HOE);

        // (Коптильня переехала к Кузнецу — см. секцию КУЗНЕЦ.)

        // ---------------- Тир 1 (CARPENTER 1): базовая столярка ----------------

        // Кнопки — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_BUTTON, Items.BIRCH_BUTTON, Items.SPRUCE_BUTTON,
                Items.JUNGLE_BUTTON, Items.ACACIA_BUTTON, Items.DARK_OAK_BUTTON,
                Items.MANGROVE_BUTTON, Items.CHERRY_BUTTON, Items.BAMBOO_BUTTON,
                Items.CRIMSON_BUTTON, Items.WARPED_BUTTON);

        // Нажимные плиты, лестница — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.OAK_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE,
                Items.JUNGLE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE,
                Items.MANGROVE_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE,
                Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.LADDER, Items.FISHING_ROD);

        // Двери, люки — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.OAK_DOOR, Items.BIRCH_DOOR, Items.SPRUCE_DOOR,
                Items.JUNGLE_DOOR, Items.ACACIA_DOOR, Items.DARK_OAK_DOOR,
                Items.MANGROVE_DOOR, Items.CHERRY_DOOR, Items.BAMBOO_DOOR,
                Items.CRIMSON_DOOR, Items.WARPED_DOOR);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.OAK_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.SPRUCE_TRAPDOOR,
                Items.JUNGLE_TRAPDOOR, Items.ACACIA_TRAPDOOR, Items.DARK_OAK_TRAPDOOR,
                Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR, Items.BAMBOO_TRAPDOOR,
                Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR);

        // Бочка — XP 3 (каменные инструменты переехали к Кузнецу)
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f),
                Items.BARREL);

        // Кровати — XP 3
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f),
                Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED,
                Items.LIGHT_BLUE_BED, Items.YELLOW_BED, Items.LIME_BED,
                Items.PINK_BED, Items.GRAY_BED, Items.LIGHT_GRAY_BED,
                Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
                Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED, Items.BLACK_BED);

        // ------------- Тир 2 (CARPENTER 2): формовка и декор -------------

        // Плиты, ступени (+ бамбук, незер, мозаика) — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_SLAB, Items.BIRCH_SLAB, Items.SPRUCE_SLAB,
                Items.JUNGLE_SLAB, Items.ACACIA_SLAB, Items.DARK_OAK_SLAB,
                Items.MANGROVE_SLAB, Items.CHERRY_SLAB, Items.BAMBOO_SLAB,
                Items.CRIMSON_SLAB, Items.WARPED_SLAB, Items.BAMBOO_MOSAIC_SLAB);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_STAIRS, Items.BIRCH_STAIRS, Items.SPRUCE_STAIRS,
                Items.JUNGLE_STAIRS, Items.ACACIA_STAIRS, Items.DARK_OAK_STAIRS,
                Items.MANGROVE_STAIRS, Items.CHERRY_STAIRS, Items.BAMBOO_STAIRS,
                Items.CRIMSON_STAIRS, Items.WARPED_STAIRS, Items.BAMBOO_MOSAIC_STAIRS);

        // Заборы, калитки — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.OAK_FENCE, Items.BIRCH_FENCE, Items.SPRUCE_FENCE,
                Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
                Items.MANGROVE_FENCE, Items.CHERRY_FENCE, Items.BAMBOO_FENCE,
                Items.CRIMSON_FENCE, Items.WARPED_FENCE);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.OAK_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.SPRUCE_FENCE_GATE,
                Items.JUNGLE_FENCE_GATE, Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE,
                Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE, Items.BAMBOO_FENCE_GATE,
                Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE);

        // Висячие таблички — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_HANGING_SIGN, Items.BIRCH_HANGING_SIGN, Items.SPRUCE_HANGING_SIGN,
                Items.JUNGLE_HANGING_SIGN, Items.ACACIA_HANGING_SIGN, Items.DARK_OAK_HANGING_SIGN,
                Items.MANGROVE_HANGING_SIGN, Items.CHERRY_HANGING_SIGN, Items.BAMBOO_HANGING_SIGN,
                Items.CRIMSON_HANGING_SIGN, Items.WARPED_HANGING_SIGN);

        // Бамбук-блоки, строительная подмога — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK,
                Items.BAMBOO_MOSAIC, Items.SCAFFOLDING);

        // Блоки коры (Wood/Hyphae) — крафт из 4 брёвен — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.OAK_WOOD, Items.BIRCH_WOOD, Items.SPRUCE_WOOD,
                Items.JUNGLE_WOOD, Items.ACACIA_WOOD, Items.DARK_OAK_WOOD,
                Items.MANGROVE_WOOD, Items.CHERRY_WOOD,
                Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.STRIPPED_OAK_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_SPRUCE_WOOD,
                Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_DARK_OAK_WOOD,
                Items.STRIPPED_MANGROVE_WOOD, Items.STRIPPED_CHERRY_WOOD,
                Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE);

        // Декор «не очень деревянное» — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.COMPOSTER, Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME,
                Items.PAINTING, Items.ARMOR_STAND);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER,
                Items.LIGHT_BLUE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER,
                Items.PINK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER,
                Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
                Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER);

        // ------- Тир 3 (CARPENTER 3): транспорт, мебель, станции -------

        // Лодки, лодки с сундуком — XP 4
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f),
                Items.OAK_BOAT, Items.BIRCH_BOAT, Items.SPRUCE_BOAT,
                Items.JUNGLE_BOAT, Items.ACACIA_BOAT, Items.DARK_OAK_BOAT,
                Items.MANGROVE_BOAT, Items.CHERRY_BOAT, Items.BAMBOO_RAFT);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f),
                Items.OAK_CHEST_BOAT, Items.BIRCH_CHEST_BOAT, Items.SPRUCE_CHEST_BOAT,
                Items.JUNGLE_CHEST_BOAT, Items.ACACIA_CHEST_BOAT, Items.DARK_OAK_CHEST_BOAT,
                Items.MANGROVE_CHEST_BOAT, Items.CHERRY_CHEST_BOAT, Items.BAMBOO_CHEST_RAFT);

        // Книжные полки, улей — XP 4
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f),
                Items.BOOKSHELF, Items.CHISELED_BOOKSHELF, Items.BEEHIVE);

        // Рабочие станции профессий, костры — XP 5
        // (Кузнечный стол переехал к Кузнецу — см. секцию КУЗНЕЦ.)
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 5f),
                Items.LECTERN, Items.LOOM, Items.FLETCHING_TABLE,
                Items.CARTOGRAPHY_TABLE);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 5f),
                Items.CAMPFIRE, Items.SOUL_CAMPFIRE);

        // ================================================================
        // КУЗНЕЦ
        // ================================================================
        // Шанса провала у Кузнеца НЕТ — всё крафтится всегда успешно (в пределах
        // открытого тира, baseChance=1.0). Прогрессия держится на лестнице доступа
        // (ItemCraftTierMap) и на ЗАКАЛКЕ: прочность изделия растёт с уровнем
        // (BlacksmithTemperMap, см. SpecializationEffects.applyBlacksmithTempering).
        // Незеритовый ГИР делается на кузнечном столе (SmithingMenuMixin), здесь его нет.
        // Тиры доступа: медь ур.1, золото ур.2, железо ур.3, алмаз ур.5, незерит ур.7.

        // ---- Каменные инструменты (без гейта, ур.0) ----
        // Нужны для раннего прогресса (каменной киркой добывают железо), поэтому
        // не гейтятся — но XP уже идёт Кузнецу. Закалке не подлежат (дёшево).
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 3f),
                Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_PICKAXE,
                Items.STONE_SHOVEL, Items.STONE_HOE);

        // ---- Медь (нижний металл, тир 1) ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 3f),
                Items.LIGHTNING_ROD, Items.SPYGLASS);

        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 2f),
                Items.BRUSH);

        // Медные декоративные блоки 1.21 (chiseled/grate/bulb/door/trapdoor во всех
        // степенях окисления + восковые) — медь, XP 2
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 2f),
                Items.CHISELED_COPPER, Items.EXPOSED_CHISELED_COPPER,
                Items.WEATHERED_CHISELED_COPPER, Items.OXIDIZED_CHISELED_COPPER,
                Items.WAXED_CHISELED_COPPER, Items.WAXED_EXPOSED_CHISELED_COPPER,
                Items.WAXED_WEATHERED_CHISELED_COPPER, Items.WAXED_OXIDIZED_CHISELED_COPPER,
                Items.COPPER_GRATE, Items.EXPOSED_COPPER_GRATE,
                Items.WEATHERED_COPPER_GRATE, Items.OXIDIZED_COPPER_GRATE,
                Items.WAXED_COPPER_GRATE, Items.WAXED_EXPOSED_COPPER_GRATE,
                Items.WAXED_WEATHERED_COPPER_GRATE, Items.WAXED_OXIDIZED_COPPER_GRATE,
                Items.COPPER_BULB, Items.EXPOSED_COPPER_BULB,
                Items.WEATHERED_COPPER_BULB, Items.OXIDIZED_COPPER_BULB,
                Items.WAXED_COPPER_BULB, Items.WAXED_EXPOSED_COPPER_BULB,
                Items.WAXED_WEATHERED_COPPER_BULB, Items.WAXED_OXIDIZED_COPPER_BULB,
                Items.COPPER_DOOR, Items.EXPOSED_COPPER_DOOR,
                Items.WEATHERED_COPPER_DOOR, Items.OXIDIZED_COPPER_DOOR,
                Items.WAXED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_DOOR,
                Items.WAXED_WEATHERED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_DOOR,
                Items.COPPER_TRAPDOOR, Items.EXPOSED_COPPER_TRAPDOOR,
                Items.WEATHERED_COPPER_TRAPDOOR, Items.OXIDIZED_COPPER_TRAPDOOR,
                Items.WAXED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR,
                Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);

        // Полированная медь (cut copper) — блок/плита/ступени во всех состояниях, XP 2
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 2f),
                Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER,
                Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER,
                Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER,
                Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER,
                Items.CUT_COPPER_SLAB, Items.EXPOSED_CUT_COPPER_SLAB,
                Items.WEATHERED_CUT_COPPER_SLAB, Items.OXIDIZED_CUT_COPPER_SLAB,
                Items.WAXED_CUT_COPPER_SLAB, Items.WAXED_EXPOSED_CUT_COPPER_SLAB,
                Items.WAXED_WEATHERED_CUT_COPPER_SLAB, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB,
                Items.CUT_COPPER_STAIRS, Items.EXPOSED_CUT_COPPER_STAIRS,
                Items.WEATHERED_CUT_COPPER_STAIRS, Items.OXIDIZED_CUT_COPPER_STAIRS,
                Items.WAXED_CUT_COPPER_STAIRS, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);

        // Черепаший шлем — крафт из скюта (натуральный материал), XP 3; носит Воин
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.TURTLE_HELMET);

        // ---- Золото (тир 2): инструменты + броня ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 15f),
                Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_PICKAXE,
                Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);

        // Золотые приборы/утилитарка — тир 2
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 5f),
                Items.CLOCK, Items.LIGHT_WEIGHTED_PRESSURE_PLATE);

        // ---- Железо (тир 3): инструменты + броня ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 20f),
                Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE,
                Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS, Items.IRON_BOOTS);

        // Кольчужная броня — железный тир
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 18f),
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
                Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);

        // Железная утилитарка
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 15f),
                Items.IRON_DOOR, Items.IRON_TRAPDOOR,
                Items.IRON_BARS, Items.CAULDRON,
                Items.ANVIL, Items.STONECUTTER,
                Items.SHEARS, Items.BUCKET,
                Items.SHIELD, Items.FLINT_AND_STEEL,
                Items.COMPASS, Items.CHAIN,
                Items.LANTERN, Items.SOUL_LANTERN,
                Items.HOPPER, Items.BLAST_FURNACE, Items.GRINDSTONE,
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE);

        // Recovery compass — дорогой прибор (эхо-осколки)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 10f),
                Items.RECOVERY_COMPASS);

        // Вагонетки (железный транспорт)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 6f),
                Items.MINECART, Items.CHEST_MINECART, Items.FURNACE_MINECART,
                Items.HOPPER_MINECART, Items.TNT_MINECART);

        // ---- Алмаз (тир 5): инструменты + броня ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 40f),
                Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);

        // ---- Незерит-прочее (тир 7); сам гир — на кузнечном столе ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 20f),
                Items.NETHERITE_INGOT);

        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 15f),
                Items.LODESTONE);

        // Станции и металлические механизмы (тир 2: коптильня/плавильня; прочее без гейта)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 5f),
                Items.SMOKER, Items.SMITHING_TABLE,
                Items.FURNACE, Items.DISPENSER, Items.DROPPER,
                Items.OBSERVER, Items.PISTON, Items.STICKY_PISTON);

        // Лук, стрелы, арбалет
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 3f),
                Items.BOW, Items.ARROW);

        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 10f),
                Items.CROSSBOW);

        // ================================================================
        // МАСТЕРОВОЙ — натуральные материалы (кожа, шерсть, глина, керамика)
        // ================================================================
        // Всё крафтится всегда успешно (baseChance=1.0); прогрессия — на лестнице
        // доступа (ItemCraftTierMap, spec=CRAFTSMAN) и активных эффектах (экономия
        // материала + двойной выход — см. SpecializationEffects). XP по ценности
        // изделия. Будущие модовые предметы из натуральных материалов (рюкзаки и
        // т.п.) добавляются сюда же. Обжиг натуральных материалов в печи даёт XP
        // отдельно (NaturalSmeltMap → XPSystem.onNaturalSmelted).

        // XP масштабируется по тиру доступа (= уровню гейта в ItemCraftTierMap):
        // базовая стройка (ур.0) = 1; чем выше требуемый уровень, тем больше XP за
        // крафт-действие. XP даётся за крафт-событие (пачка плит = 1 событие).

        // ================== Ур.0 (без гейта), XP 1 — базовая стройка ==================

        // Шерсть из нити + ковры
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL,
                Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL,
                Items.PINK_WOOL, Items.GRAY_WOOL, Items.LIGHT_GRAY_WOOL,
                Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
                Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET,
                Items.LIGHT_BLUE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET,
                Items.PINK_CARPET, Items.GRAY_CARPET, Items.LIGHT_GRAY_CARPET,
                Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
                Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET,
                Items.MOSS_CARPET);

        // Свечи (воск + нить)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.CANDLE,
                Items.WHITE_CANDLE, Items.ORANGE_CANDLE, Items.MAGENTA_CANDLE,
                Items.LIGHT_BLUE_CANDLE, Items.YELLOW_CANDLE, Items.LIME_CANDLE,
                Items.PINK_CANDLE, Items.GRAY_CANDLE, Items.LIGHT_GRAY_CANDLE,
                Items.CYAN_CANDLE, Items.PURPLE_CANDLE, Items.BLUE_CANDLE,
                Items.BROWN_CANDLE, Items.GREEN_CANDLE, Items.RED_CANDLE, Items.BLACK_CANDLE);

        // Бумага, поводок, цветочный горшок, книга с пером
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.PAPER, Items.LEAD, Items.FLOWER_POT, Items.WRITABLE_BOOK);

        // Стекло: панели + тонированное (цветное стекло — тир 3, ниже)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.GLASS_PANE, Items.TINTED_GLASS);

        // Бетонная пудра (песок + гравий + краситель)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.WHITE_CONCRETE_POWDER, Items.ORANGE_CONCRETE_POWDER, Items.MAGENTA_CONCRETE_POWDER,
                Items.LIGHT_BLUE_CONCRETE_POWDER, Items.YELLOW_CONCRETE_POWDER, Items.LIME_CONCRETE_POWDER,
                Items.PINK_CONCRETE_POWDER, Items.GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_CONCRETE_POWDER,
                Items.CYAN_CONCRETE_POWDER, Items.PURPLE_CONCRETE_POWDER, Items.BLUE_CONCRETE_POWDER,
                Items.BROWN_CONCRETE_POWDER, Items.GREEN_CONCRETE_POWDER, Items.RED_CONCRETE_POWDER,
                Items.BLACK_CONCRETE_POWDER);

        // Глина (XP 2 — базовое сырьё, чуть дороже) + прочие натуральные блоки
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f),
                Items.CLAY);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.PACKED_MUD, Items.COARSE_DIRT,
                Items.BONE_BLOCK, Items.HONEYCOMB_BLOCK, Items.SLIME_BLOCK,
                Items.POLISHED_BASALT);

        // Кирпич / незер-кирпич / грязевой кирпич (резной незер-кирпич — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.BRICKS, Items.BRICK_SLAB, Items.BRICK_STAIRS, Items.BRICK_WALL,
                Items.NETHER_BRICKS, Items.NETHER_BRICK_SLAB, Items.NETHER_BRICK_STAIRS,
                Items.NETHER_BRICK_WALL, Items.NETHER_BRICK_FENCE,
                Items.RED_NETHER_BRICKS, Items.RED_NETHER_BRICK_SLAB, Items.RED_NETHER_BRICK_STAIRS,
                Items.RED_NETHER_BRICK_WALL,
                Items.MUD_BRICKS, Items.MUD_BRICK_SLAB, Items.MUD_BRICK_STAIRS, Items.MUD_BRICK_WALL);

        // Камень / каменный кирпич / булыжник (резьба и мшистость — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.STONE_SLAB, Items.STONE_STAIRS, Items.SMOOTH_STONE_SLAB,
                Items.STONE_BRICKS, Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS,
                Items.STONE_BRICK_WALL,
                Items.COBBLESTONE_SLAB, Items.COBBLESTONE_STAIRS, Items.COBBLESTONE_WALL);

        // Андезит / диорит / гранит (полировка + плиты/ступени/стены)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.POLISHED_ANDESITE, Items.POLISHED_ANDESITE_SLAB, Items.POLISHED_ANDESITE_STAIRS,
                Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL,
                Items.POLISHED_DIORITE, Items.POLISHED_DIORITE_SLAB, Items.POLISHED_DIORITE_STAIRS,
                Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL,
                Items.POLISHED_GRANITE, Items.POLISHED_GRANITE_SLAB, Items.POLISHED_GRANITE_STAIRS,
                Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL);

        // Песчаник / красный песчаник (резьба — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.SANDSTONE, Items.SANDSTONE_SLAB, Items.SANDSTONE_STAIRS, Items.SANDSTONE_WALL,
                Items.CUT_SANDSTONE, Items.CUT_STANDSTONE_SLAB, Items.SMOOTH_SANDSTONE_SLAB,
                Items.SMOOTH_SANDSTONE_STAIRS,
                Items.RED_SANDSTONE, Items.RED_SANDSTONE_SLAB, Items.RED_SANDSTONE_STAIRS,
                Items.RED_SANDSTONE_WALL, Items.CUT_RED_SANDSTONE, Items.CUT_RED_SANDSTONE_SLAB,
                Items.SMOOTH_RED_SANDSTONE_SLAB, Items.SMOOTH_RED_SANDSTONE_STAIRS);

        // Туф 1.21 (полировка/кирпич/плиты/ступени/стены) — резьба chiseled на тире 4
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.TUFF_SLAB, Items.TUFF_STAIRS, Items.TUFF_WALL,
                Items.POLISHED_TUFF, Items.POLISHED_TUFF_SLAB,
                Items.POLISHED_TUFF_STAIRS, Items.POLISHED_TUFF_WALL,
                Items.TUFF_BRICKS, Items.TUFF_BRICK_SLAB,
                Items.TUFF_BRICK_STAIRS, Items.TUFF_BRICK_WALL);

        // ================== Тир 1 (CRAFTSMAN 1): кожа, книги ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f),
                Items.LEATHER, Items.BOOK);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.LEATHER_BOOTS, Items.LEATHER_HELMET);

        // ================== Тир 2 (CRAFTSMAN 2): тяжёлая кожа + терракота ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f),
                Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
                Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
                Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
                Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
                Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA);

        // ================== Тир 3 (CRAFTSMAN 3): decorated pot + витражи ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
                Items.DECORATED_POT);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f),
                Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS,
                Items.LIGHT_BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS,
                Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS,
                Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
                Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS,
                Items.BLACK_STAINED_GLASS,
                Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE,
                Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE,
                Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE,
                Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
                Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE,
                Items.BLACK_STAINED_GLASS_PANE);

        // ================== Тир 4 (CRAFTSMAN 4): узорная отделка камня, XP 3 ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.CHISELED_STONE_BRICKS, Items.CHISELED_SANDSTONE, Items.CHISELED_RED_SANDSTONE,
                Items.CHISELED_NETHER_BRICKS, Items.CHISELED_TUFF, Items.CHISELED_TUFF_BRICKS,
                Items.MOSSY_STONE_BRICKS, Items.MOSSY_STONE_BRICK_SLAB,
                Items.MOSSY_STONE_BRICK_STAIRS, Items.MOSSY_STONE_BRICK_WALL,
                Items.MOSSY_COBBLESTONE_SLAB, Items.MOSSY_COBBLESTONE_STAIRS, Items.MOSSY_COBBLESTONE_WALL);

        // ================== Тир 5 (CRAFTSMAN 5): дипслейт, XP 4 ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
                Items.COBBLED_DEEPSLATE_SLAB, Items.COBBLED_DEEPSLATE_STAIRS, Items.COBBLED_DEEPSLATE_WALL,
                Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE_SLAB,
                Items.POLISHED_DEEPSLATE_STAIRS, Items.POLISHED_DEEPSLATE_WALL,
                Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_BRICK_SLAB,
                Items.DEEPSLATE_BRICK_STAIRS, Items.DEEPSLATE_BRICK_WALL,
                Items.DEEPSLATE_TILES, Items.DEEPSLATE_TILE_SLAB,
                Items.DEEPSLATE_TILE_STAIRS, Items.DEEPSLATE_TILE_WALL,
                Items.CHISELED_DEEPSLATE);

        // ================== Тир 6 (CRAFTSMAN 6): блэкстоун + кварц, XP 5 ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 5f),
                Items.BLACKSTONE_SLAB, Items.BLACKSTONE_STAIRS, Items.BLACKSTONE_WALL,
                Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_SLAB,
                Items.POLISHED_BLACKSTONE_STAIRS, Items.POLISHED_BLACKSTONE_WALL,
                Items.POLISHED_BLACKSTONE_BUTTON, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                Items.POLISHED_BLACKSTONE_BRICKS, Items.POLISHED_BLACKSTONE_BRICK_SLAB,
                Items.POLISHED_BLACKSTONE_BRICK_STAIRS, Items.POLISHED_BLACKSTONE_BRICK_WALL,
                Items.CHISELED_POLISHED_BLACKSTONE,
                Items.QUARTZ_BLOCK, Items.QUARTZ_BRICKS, Items.QUARTZ_PILLAR,
                Items.QUARTZ_SLAB, Items.QUARTZ_STAIRS, Items.CHISELED_QUARTZ_BLOCK,
                Items.SMOOTH_QUARTZ_SLAB, Items.SMOOTH_QUARTZ_STAIRS);

        // ================== Тир 7 (CRAFTSMAN 7): призмарин/пурпур/энд-камень, XP 6 ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 6f),
                Items.PRISMARINE_SLAB, Items.PRISMARINE_STAIRS, Items.PRISMARINE_WALL,
                Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICK_SLAB, Items.PRISMARINE_BRICK_STAIRS,
                Items.DARK_PRISMARINE, Items.DARK_PRISMARINE_SLAB, Items.DARK_PRISMARINE_STAIRS,
                Items.PURPUR_BLOCK, Items.PURPUR_PILLAR, Items.PURPUR_SLAB, Items.PURPUR_STAIRS,
                Items.END_STONE_BRICKS, Items.END_STONE_BRICK_SLAB,
                Items.END_STONE_BRICK_STAIRS, Items.END_STONE_BRICK_WALL);

        // ================================================================
        // ПРОМЫСЕЛ — ПОВАР
        // ================================================================
        // Крафт еды всегда успешен (baseChance=1.0). Гейтинг доступа — по
        // уровню Повара через FoodTierMap (см. XPSystem.onCraft). XP по тирам
        // ценности; составные блюда дороже готовки в печи (см. FoodCookMap).

        // Базовое
        register(new CraftEntry(Path.HARVEST, Spec.COOK, 2f),
                Items.COOKIE, Items.BREAD);

        // Супы
        register(new CraftEntry(Path.HARVEST, Spec.COOK, 4f),
                Items.MUSHROOM_STEW, Items.BEETROOT_SOUP, Items.SUSPICIOUS_STEW);

        // Тыквенный пирог
        register(new CraftEntry(Path.HARVEST, Spec.COOK, 6f),
                Items.PUMPKIN_PIE);

        // Рагу из кролика
        register(new CraftEntry(Path.HARVEST, Spec.COOK, 8f),
                Items.RABBIT_STEW);

        // Торт
        register(new CraftEntry(Path.HARVEST, Spec.COOK, 12f),
                Items.CAKE);

        // ================================================================
        // МАГИЯ — АЛХИМИК
        // ================================================================
        // Крафт магических расходников/реагентов — всегда успех (baseChance=1.0),
        // XP идёт в путь Магия. Гейтинг доступа (где есть) — ItemCraftTierMap.

        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 4f),
                Items.BLAZE_POWDER);

        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 2f),
                Items.GLASS_BOTTLE);

        // Реагенты варки
        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 3f),
                Items.FERMENTED_SPIDER_EYE, Items.GLISTERING_MELON_SLICE,
                Items.MAGMA_CREAM);

        // ================================================================
        // МАГИЯ — ЗАЧАРОВАТЕЛЬ
        // ================================================================
        // Стол зачарования — профильный инструмент Зачарователя, крафтабелен
        // с ур.0 (без гейта), чтобы было на чём прокачиваться. XP идёт в Магию.
        register(new CraftEntry(Path.MAGIC, Spec.ENCHANTER, 8f),
                Items.ENCHANTING_TABLE);

        // Красители (любой источник — XP мал, абуз незначителен)
        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 0.5f),
                Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE,
                Items.LIGHT_BLUE_DYE, Items.YELLOW_DYE, Items.LIME_DYE,
                Items.PINK_DYE, Items.GRAY_DYE, Items.LIGHT_GRAY_DYE,
                Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
                Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE);
    }

    private static void register(CraftEntry entry, Item... items) {
        for (Item item : items) {
            MAP.put(item, entry);
        }
    }

    public static CraftEntry get(Item item) {
        if (!initialized) init();
        return MAP.get(item);
    }
}