package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import com.kingdomrp.core.registry.KRPItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Маппинг получения опыта за крафт предметов соответствующих путей/специализаций
public class ItemCraftMap {

    private static final Map<Item, CraftEntry> MAP = new HashMap<>();
    /** Тег-правила (fallback после точных Item; мод-совместимость: modded-варианты). */
    private static final List<Map.Entry<TagKey<Item>, CraftEntry>> TAGS = new ArrayList<>();
    private static boolean initialized = false;

    /** Тег предметов по ID (для common-тегов вроде c:bookshelves). */
    private static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        initCraftPath();
        initHarvestPath();
        initMagicPath();
    }

    // Путь "Ремесло"
    private static void initCraftPath() {
        initCarpenter();
        initBlacksmith();
        initCraftsman();
    }

    // Специализация "Плотник"
    private static void initCarpenter() {
        // ---------------- Тир 0 (без гейта): минимум для старта ----------------
        // Палка, миска — XP 1
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f),
                Items.STICK, Items.BOWL);

        // Сундук, деревянные инструменты — XP 2 (верстак — тег c:...:crafting_tables)
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.CHEST);

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_PICKAXE,
                Items.WOODEN_SHOVEL, Items.WOODEN_HOE);

        // Лестница, удочка — XP 2
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f),
                Items.LADDER, Items.FISHING_ROD);

        // Бочка — XP 3
        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f),
                Items.BARREL);

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

        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f),
                Items.COMPOSTER, Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME,
                Items.PAINTING, Items.ARMOR_STAND);


        register(new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f),
                Items.LECTERN, Items.LOOM, Items.FLETCHING_TABLE, Items.CARTOGRAPHY_TABLE,
                Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.BEEHIVE);

        // ТЕГ-FALLBACK (мод-совместимость) — Плотник
        // Ловят modded-варианты, которым точный Items-ключ не соответствует.
        // Проверяются ПОСЛЕ точных Item, XP совпадает с ванильными семействами.
        registerTag(ItemTags.PLANKS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f));
        registerTag(ItemTags.WOODEN_SLABS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1.5f));
        registerTag(ItemTags.WOODEN_STAIRS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.WOODEN_BUTTONS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 0.5f));
        registerTag(ItemTags.SIGNS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f));
        registerTag(ItemTags.HANGING_SIGNS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f));
        registerTag(ItemTags.WOODEN_DOORS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.WOODEN_TRAPDOORS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.WOODEN_FENCES, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.FENCE_GATES, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.BANNERS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.WOODEN_PRESSURE_PLATES, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1f));
        registerTag(ItemTags.BEDS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 3f));
        registerTag(ItemTags.BOATS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(ItemTags.CHEST_BOATS, new CraftEntry(Path.CRAFT, Spec.CARPENTER, 2f));
        registerTag(itemTag("c:bookshelves"), new CraftEntry(Path.CRAFT, Spec.CARPENTER, 4f));
        registerTag(itemTag("c:player_workstations/crafting_tables"), new CraftEntry(Path.CRAFT, Spec.CARPENTER, 1.5f));
    }

    // Специализация "Кузнец"
    private static void initBlacksmith() {
        // ---- Каменные инструменты (без гейта, ур.0) — XP по затратам ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 2f),
                Items.STONE_SHOVEL);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 3f),
                Items.STONE_HOE, Items.STONE_SWORD);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 4f),
                Items.STONE_PICKAXE, Items.STONE_AXE);

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

        // ---- Золото (тир 2): инструменты + броня ----
        registerGear(3f, 8f, 12f,
                Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD,
                Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE,
                Items.GOLDEN_BOOTS, Items.GOLDEN_HELMET,
                Items.GOLDEN_LEGGINGS, Items.GOLDEN_CHESTPLATE);

        // Золотые приборы/утилитарка — тир 2
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 5f),
                Items.CLOCK, Items.LIGHT_WEIGHTED_PRESSURE_PLATE);

        // ---- Железо (тир 3): инструменты + броня ----
        registerGear(4f, 10f, 14f,
                Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD,
                Items.IRON_PICKAXE, Items.IRON_AXE,
                Items.IRON_BOOTS, Items.IRON_HELMET,
                Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE);

        // Железное кольцо — компонент кольчуги (из самородков), XP 2
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 2f),
                KRPItems.IRON_RING.get());

        // Кольчужная броня — из железных колец (тир 2) — XP по затратам
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 9f),
                Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_HELMET);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 13f),
                Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_CHESTPLATE);

        // Железная утилитарка — XP по затратам железа
        // Дёшево (~1 слиток / самородки)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 3f),
                Items.FLINT_AND_STEEL, Items.CHAIN,
                Items.LANTERN, Items.SOUL_LANTERN);

        // 2–4 слитка
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 5f),
                Items.SHEARS, Items.BUCKET, Items.STONECUTTER,
                Items.SHIELD, Items.GRINDSTONE, Items.IRON_TRAPDOOR,
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE);

        // 5–7 слитков
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 8f),
                Items.IRON_DOOR, Items.IRON_BARS, Items.COMPASS,
                Items.CAULDRON, Items.HOPPER, Items.BLAST_FURNACE);

        // Наковальня — ~31 слиток (3 блока + 4), отдельно
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 25f),
                Items.ANVIL);

        // Recovery compass — дорогой прибор (эхо-осколки)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 10f),
                Items.RECOVERY_COMPASS);

        // Вагонетки (железный транспорт)
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 6f),
                Items.MINECART, Items.CHEST_MINECART, Items.FURNACE_MINECART,
                Items.HOPPER_MINECART, Items.TNT_MINECART);

        // ---- Алмаз (тир 5): инструменты + броня ----
        registerGear(6f, 16f, 20f,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD,
                Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
                Items.DIAMOND_BOOTS, Items.DIAMOND_HELMET,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE);

        // ---- Незерит-прочее (тир 7); сам гир — на кузнечном столе ----
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 25f),
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

        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, 4f),
                Items.CROSSBOW);
    }

    // Специализация "Мастеровой" (натуральные материалы - кожа, шерсть, глина, керамика и т.д.)
    private static void initCraftsman() {
        // Черепаший шлем — крафт из скюта (натуральный материал), XP 3; носит Воин
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.TURTLE_HELMET);

        // ================== Ур.0 (без гейта), XP 1 — базовая стройка ==================

        // Мох-ковёр (не входит в wool_carpets), тонированное стекло (не панель)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.MOSS_CARPET, Items.TINTED_GLASS);

        // Бумага, поводок, цветочный горшок, книга с пером
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.PAPER, Items.LEAD, Items.FLOWER_POT, Items.WRITABLE_BOOK);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f),
                Items.PACKED_MUD, Items.COARSE_DIRT,
                Items.BONE_BLOCK, Items.HONEYCOMB_BLOCK, Items.SLIME_BLOCK,
                Items.POLISHED_BASALT);

        // Кирпич / незер-кирпич / грязевой кирпич (резной незер-кирпич — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.BRICKS, Items.BRICK_SLAB, Items.BRICK_STAIRS, Items.BRICK_WALL,
                Items.NETHER_BRICKS, Items.NETHER_BRICK_SLAB, Items.NETHER_BRICK_STAIRS,
                Items.NETHER_BRICK_WALL, Items.NETHER_BRICK_FENCE,
                Items.RED_NETHER_BRICKS, Items.RED_NETHER_BRICK_SLAB, Items.RED_NETHER_BRICK_STAIRS,
                Items.RED_NETHER_BRICK_WALL,
                Items.MUD_BRICKS, Items.MUD_BRICK_SLAB, Items.MUD_BRICK_STAIRS, Items.MUD_BRICK_WALL);

        // Камень / каменный кирпич / булыжник (резьба и мшистость — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.STONE_SLAB, Items.STONE_STAIRS, Items.SMOOTH_STONE_SLAB,
                Items.STONE_BRICKS, Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS,
                Items.STONE_BRICK_WALL,
                Items.COBBLESTONE_SLAB, Items.COBBLESTONE_STAIRS, Items.COBBLESTONE_WALL);

        // Андезит / диорит / гранит (полировка + плиты/ступени/стены)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.POLISHED_ANDESITE, Items.POLISHED_ANDESITE_SLAB, Items.POLISHED_ANDESITE_STAIRS,
                Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL,
                Items.POLISHED_DIORITE, Items.POLISHED_DIORITE_SLAB, Items.POLISHED_DIORITE_STAIRS,
                Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL,
                Items.POLISHED_GRANITE, Items.POLISHED_GRANITE_SLAB, Items.POLISHED_GRANITE_STAIRS,
                Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL);

        // Песчаник / красный песчаник (резьба — тир 4)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 3f),
                Items.SANDSTONE, Items.SANDSTONE_SLAB, Items.SANDSTONE_STAIRS, Items.SANDSTONE_WALL,
                Items.CUT_SANDSTONE, Items.CUT_STANDSTONE_SLAB, Items.SMOOTH_SANDSTONE_SLAB,
                Items.SMOOTH_SANDSTONE_STAIRS,
                Items.RED_SANDSTONE, Items.RED_SANDSTONE_SLAB, Items.RED_SANDSTONE_STAIRS,
                Items.RED_SANDSTONE_WALL, Items.CUT_RED_SANDSTONE, Items.CUT_RED_SANDSTONE_SLAB,
                Items.SMOOTH_RED_SANDSTONE_SLAB, Items.SMOOTH_RED_SANDSTONE_STAIRS);

        // Туф 1.21 (полировка/кирпич/плиты/ступени/стены) — резьба chiseled на тире 4
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
                Items.TUFF_SLAB, Items.TUFF_STAIRS, Items.TUFF_WALL,
                Items.POLISHED_TUFF, Items.POLISHED_TUFF_SLAB,
                Items.POLISHED_TUFF_STAIRS, Items.POLISHED_TUFF_WALL,
                Items.TUFF_BRICKS, Items.TUFF_BRICK_SLAB,
                Items.TUFF_BRICK_STAIRS, Items.TUFF_BRICK_WALL);

        // ================== Тир 1 (CRAFTSMAN 1): кожа, книги ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f),
                Items.LEATHER, Items.BOOK);

        // Кожаная броня — XP по затратам (ботинки/шлем < поножи/нагрудник)
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
                Items.LEATHER_BOOTS, Items.LEATHER_HELMET);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 6f),
                Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS);

        // Седло — кожа + железо (натуральная кожа → Мастеровой), XP 5
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 5f),
                Items.SADDLE);

        // ================== Тир 3 (CRAFTSMAN 3): decorated pot + витражи ==================
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
                Items.DECORATED_POT);

        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
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
        register(new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f),
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

        // ТЕГ-FALLBACK (мод-совместимость) — Мастеровой.
        // Крашеное стекло/панели (XP 2) регистрируются поимённо выше → перекрывают
        // общий c:glass_panes (XP 1) для крашеных.
        registerTag(ItemTags.WOOL, new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f));
        registerTag(ItemTags.WOOL_CARPETS, new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f));
        registerTag(ItemTags.CANDLES, new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 1f));
        registerTag(itemTag("c:concrete_powders"), new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 4f));
        registerTag(itemTag("c:glass_panes"), new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f));
        registerTag(ItemTags.TERRACOTTA, new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, 2f));
    }

    // Путь "Промысел"
    private static void initHarvestPath() {
        initCook();
    }

    // Специализация "Повар"
    private static void initCook() {
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
    }

    // Путь "Магия"
    private static void initMagicPath() {
        initAlchemist();
        initEnchanter();
    }

    // Специализация "Алхимик"
    private static void initAlchemist() {
        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 4f),
                Items.BLAZE_POWDER);

        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 2f),
                Items.GLASS_BOTTLE);

        // Реагенты варки
        register(new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 3f),
                Items.FERMENTED_SPIDER_EYE, Items.GLISTERING_MELON_SLICE,
                Items.MAGMA_CREAM);

        // Красители (XP мал, абуз незначителен)
        registerTag(itemTag("c:dyes"), new CraftEntry(Path.MAGIC, Spec.ALCHEMIST, 0.5f));
    }

    // Специализация "Зачарователь"
    private static void initEnchanter() {
        // Стол зачарования — профильный инструмент Зачарователя, крафтабелен
        // с ур.0 (без гейта), чтобы было на чём прокачиваться. XP идёт в Магию.
        register(new CraftEntry(Path.MAGIC, Spec.ENCHANTER, 15f),
                Items.ENCHANTING_TABLE);
    }

    private static void register(CraftEntry entry, Item... items) {
        for (Item item : items) {
            MAP.put(item, entry);
        }
    }

    /**
     * Кузнечный комплект (инструменты + броня) — XP пропорционально затратам металла.
     * Инструменты: лопата=t (1 слиток), мотыга/меч=t+1 (2), кирка/топор=t+2 (3).
     * Броня: ботинки/шлем=l (4–5), поножи/нагрудник=h (7–8).
     */
    private static void registerGear(float t, float l, float h,
            Item shovel, Item hoe, Item sword, Item pickaxe, Item axe,
            Item boots, Item helmet, Item leggings, Item chestplate) {
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, t), shovel);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, t + 1), hoe, sword);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, t + 2), pickaxe, axe);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, l), boots, helmet);
        register(new CraftEntry(Path.CRAFT, Spec.BLACKSMITH, h), leggings, chestplate);
    }

    private static void registerTag(TagKey<Item> tag, CraftEntry entry) {
        TAGS.add(Map.entry(tag, entry));
    }

    public static CraftEntry get(Item item) {
        if (!initialized) init();
        CraftEntry direct = MAP.get(item);
        if (direct != null) return direct;
        for (var e : TAGS) {
            if (item.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        return null;
    }

    /** Регистрация XP за крафт по ID (мод-совместимость). No-op если предмета нет. */
    public static void addById(String id, CraftEntry entry) {
        if (!initialized) init();
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, entry));
    }

}