package com.kingdomrp.core.data;

import com.kingdomrp.core.registry.KRPItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Гейтинг КРАФТА по уровню специализаций. Значение — список требований
 * {@link SpecRequirement}, выполнены должны быть ВСЕ (поддержка dual-spec).
 * Спек берётся из профессии, которая логически делает предмет (как в
 * {@link ItemCraftMap}), а не из пути целиком.
 * <p>
 * Ношение/использование гейтится отдельно ({@link ItemUseTierMap}).
 */
public class ItemCraftTierMap {

    private static final Map<Item, List<SpecRequirement>> MAP = new HashMap<>();

    static {
        // ============================================================
        // ПЛОТНИК — гейтинг стройки по образцу старого KRP.
        // Ур.0 (БЕЗ гейта, здесь не перечислено): доски, палка, миска, знаки,
        // верстак, сундук, деревянные инструменты, коптильня (временно).
        // XP см. ItemCraftMap.
        // ============================================================

        // Тир 1: кнопки, нажимные плиты, лестница, двери, люки, каменные
        // инструменты, бочка, кровати
        gate(new SpecRequirement(Spec.CARPENTER, 1),
                Items.OAK_BUTTON, Items.BIRCH_BUTTON, Items.SPRUCE_BUTTON,
                Items.JUNGLE_BUTTON, Items.ACACIA_BUTTON, Items.DARK_OAK_BUTTON,
                Items.MANGROVE_BUTTON, Items.CHERRY_BUTTON, Items.BAMBOO_BUTTON,
                Items.CRIMSON_BUTTON, Items.WARPED_BUTTON,
                Items.OAK_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE,
                Items.JUNGLE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE,
                Items.MANGROVE_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE,
                Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE,
                Items.LADDER, Items.FISHING_ROD,
                Items.OAK_DOOR, Items.BIRCH_DOOR, Items.SPRUCE_DOOR,
                Items.JUNGLE_DOOR, Items.ACACIA_DOOR, Items.DARK_OAK_DOOR,
                Items.MANGROVE_DOOR, Items.CHERRY_DOOR, Items.BAMBOO_DOOR,
                Items.CRIMSON_DOOR, Items.WARPED_DOOR,
                Items.OAK_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.SPRUCE_TRAPDOOR,
                Items.JUNGLE_TRAPDOOR, Items.ACACIA_TRAPDOOR, Items.DARK_OAK_TRAPDOOR,
                Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR, Items.BAMBOO_TRAPDOOR,
                Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR,
                Items.BARREL,
                Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED,
                Items.LIGHT_BLUE_BED, Items.YELLOW_BED, Items.LIME_BED,
                Items.PINK_BED, Items.GRAY_BED, Items.LIGHT_GRAY_BED,
                Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
                Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED, Items.BLACK_BED);

        // Тир 2: плиты, ступени, заборы, калитки, висячие таблички, бамбук-блоки,
        // строительная подмога, декор (рамки/картина/стойка/композтер/баннеры)
        gate(new SpecRequirement(Spec.CARPENTER, 2),
                Items.OAK_SLAB, Items.BIRCH_SLAB, Items.SPRUCE_SLAB,
                Items.JUNGLE_SLAB, Items.ACACIA_SLAB, Items.DARK_OAK_SLAB,
                Items.MANGROVE_SLAB, Items.CHERRY_SLAB, Items.BAMBOO_SLAB,
                Items.CRIMSON_SLAB, Items.WARPED_SLAB, Items.BAMBOO_MOSAIC_SLAB,
                Items.OAK_STAIRS, Items.BIRCH_STAIRS, Items.SPRUCE_STAIRS,
                Items.JUNGLE_STAIRS, Items.ACACIA_STAIRS, Items.DARK_OAK_STAIRS,
                Items.MANGROVE_STAIRS, Items.CHERRY_STAIRS, Items.BAMBOO_STAIRS,
                Items.CRIMSON_STAIRS, Items.WARPED_STAIRS, Items.BAMBOO_MOSAIC_STAIRS,
                Items.OAK_FENCE, Items.BIRCH_FENCE, Items.SPRUCE_FENCE,
                Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
                Items.MANGROVE_FENCE, Items.CHERRY_FENCE, Items.BAMBOO_FENCE,
                Items.CRIMSON_FENCE, Items.WARPED_FENCE,
                Items.OAK_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.SPRUCE_FENCE_GATE,
                Items.JUNGLE_FENCE_GATE, Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE,
                Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE, Items.BAMBOO_FENCE_GATE,
                Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE,
                Items.OAK_HANGING_SIGN, Items.BIRCH_HANGING_SIGN, Items.SPRUCE_HANGING_SIGN,
                Items.JUNGLE_HANGING_SIGN, Items.ACACIA_HANGING_SIGN, Items.DARK_OAK_HANGING_SIGN,
                Items.MANGROVE_HANGING_SIGN, Items.CHERRY_HANGING_SIGN, Items.BAMBOO_HANGING_SIGN,
                Items.CRIMSON_HANGING_SIGN, Items.WARPED_HANGING_SIGN,
                Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK,
                Items.BAMBOO_MOSAIC, Items.SCAFFOLDING,
                Items.OAK_WOOD, Items.BIRCH_WOOD, Items.SPRUCE_WOOD,
                Items.JUNGLE_WOOD, Items.ACACIA_WOOD, Items.DARK_OAK_WOOD,
                Items.MANGROVE_WOOD, Items.CHERRY_WOOD,
                Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
                Items.STRIPPED_OAK_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_SPRUCE_WOOD,
                Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_DARK_OAK_WOOD,
                Items.STRIPPED_MANGROVE_WOOD, Items.STRIPPED_CHERRY_WOOD,
                Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE,
                Items.COMPOSTER, Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME,
                Items.PAINTING, Items.ARMOR_STAND,
                Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER,
                Items.LIGHT_BLUE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER,
                Items.PINK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER,
                Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
                Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER);

        // Тир 3: транспорт (лодки/лодки с сундуком), мебель (книжные полки/улей),
        // рабочие станции профессий, костры
        gate(new SpecRequirement(Spec.CARPENTER, 3),
                Items.OAK_BOAT, Items.BIRCH_BOAT, Items.SPRUCE_BOAT,
                Items.JUNGLE_BOAT, Items.ACACIA_BOAT, Items.DARK_OAK_BOAT,
                Items.MANGROVE_BOAT, Items.CHERRY_BOAT, Items.BAMBOO_RAFT,
                Items.OAK_CHEST_BOAT, Items.BIRCH_CHEST_BOAT, Items.SPRUCE_CHEST_BOAT,
                Items.JUNGLE_CHEST_BOAT, Items.ACACIA_CHEST_BOAT, Items.DARK_OAK_CHEST_BOAT,
                Items.MANGROVE_CHEST_BOAT, Items.CHERRY_CHEST_BOAT, Items.BAMBOO_CHEST_RAFT,
                Items.BOOKSHELF, Items.CHISELED_BOOKSHELF, Items.BEEHIVE,
                Items.LECTERN, Items.LOOM, Items.FLETCHING_TABLE,
                Items.CARTOGRAPHY_TABLE, Items.SMITHING_TABLE,
                Items.CAMPFIRE, Items.SOUL_CAMPFIRE);

        // ============================================================
        // КУЗНЕЦ — металл, инструменты, броня, утилитарка.
        // Тиры: медь ур.1, золото ур.2, железо ур.3, алмаз ур.5, незерит ур.7.
        // Незеритовый ГИР гейтится отдельно на кузнечном столе (SmithingMenuMixin),
        // здесь его нет. Незеритовый слиток/магнетит крафтятся на верстаке — ур.7.
        // ============================================================

        // Тир 1 — медь (+ декоративные медные блоки 1.21 во всех состояниях)
        gate(new SpecRequirement(Spec.BLACKSMITH, 1),
                Items.LIGHTNING_ROD, Items.SPYGLASS, Items.BRUSH,
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
                Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR,
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

        // Тир 2 — золото (инструменты + броня)
        gate(new SpecRequirement(Spec.BLACKSMITH, 2),
                Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_PICKAXE,
                Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);

        // Тир 2 — золотые приборы + ранняя утилитарка + станции переработки
        gate(new SpecRequirement(Spec.BLACKSMITH, 2),
                Items.CLOCK, Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                Items.SHEARS, Items.BUCKET, Items.FLINT_AND_STEEL,
                Items.SHIELD, Items.STONECUTTER, Items.BOW,
                Items.SMOKER, Items.BLAST_FURNACE);

        // Тир 2 — железное кольцо + кольчужная броня (из колец)
        gate(new SpecRequirement(Spec.BLACKSMITH, 2),
                KRPItems.IRON_RING.get(),
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
                Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);

        // Тир 3 — железо (инструменты + броня)
        gate(new SpecRequirement(Spec.BLACKSMITH, 3),
                Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE,
                Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS, Items.IRON_BOOTS);

        // Тир 3 — железная утилитарка, приборы, транспорт, станции, механизмы
        gate(new SpecRequirement(Spec.BLACKSMITH, 3),
                Items.IRON_DOOR, Items.IRON_TRAPDOOR, Items.IRON_BARS,
                Items.CAULDRON, Items.ANVIL, Items.GRINDSTONE, Items.HOPPER,
                Items.CHAIN, Items.LANTERN, Items.SOUL_LANTERN,
                Items.COMPASS, Items.RECOVERY_COMPASS,
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.SMITHING_TABLE,
                Items.MINECART, Items.CHEST_MINECART, Items.FURNACE_MINECART,
                Items.HOPPER_MINECART, Items.TNT_MINECART,
                Items.PISTON, Items.STICKY_PISTON,
                Items.DISPENSER, Items.DROPPER, Items.OBSERVER);

        gate(new SpecRequirement(Spec.BLACKSMITH, 4),
                Items.CROSSBOW);

        // Тир 5 — алмаз (инструменты + броня)
        gate(new SpecRequirement(Spec.BLACKSMITH, 5),
                Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);

        // Тир 7 — незеритовый слиток + магнетит (крафтятся на верстаке)
        gate(new SpecRequirement(Spec.BLACKSMITH, 7),
                Items.NETHERITE_INGOT, Items.LODESTONE);

        // ============================================================
        // МАСТЕРОВОЙ — натуральные материалы (текстиль/кожа/стекло/керамика/камень).
        // Ур.0 (БЕЗ гейта, здесь не перечислено): базовая стройка — текстиль/свечи,
        // стекло (панели/тонир.), бетон. пудра, обычная каменная кладка (камень/
        // булыжник/песчаник/кирпич/незер-кирпич/грязевой кирпич + полировка гранита/
        // диорита/андезита), глина, прочие натуральные блоки. XP см. ItemCraftMap.
        // Лестница размазана 1→7: чем «дальше/декоративнее» материал, тем выше гейт.
        // ============================================================

        // Тир 1 — выделка кожи, книги, лёгкая кожаная броня
        gate(new SpecRequirement(Spec.CRAFTSMAN, 1),
                Items.LEATHER, Items.BOOK,
                Items.LEATHER_BOOTS, Items.LEATHER_HELMET);

        // Тир 2 — тяжёлая кожаная броня + крашеная терракота + черепаший шлем (скют)
        gate(new SpecRequirement(Spec.CRAFTSMAN, 2),
                Items.TURTLE_HELMET,
                Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
                Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
                Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
                Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
                Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
                Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA);

        // Тир 3 — тонкая керамика + цветное стекло (витражи) + седло
        gate(new SpecRequirement(Spec.CRAFTSMAN, 3),
                Items.SADDLE,
                Items.DECORATED_POT,
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

        // Тир 4 — узорная отделка камня (резьба, мшистость)
        gate(new SpecRequirement(Spec.CRAFTSMAN, 4),
                Items.CHISELED_STONE_BRICKS, Items.CHISELED_SANDSTONE, Items.CHISELED_RED_SANDSTONE,
                Items.CHISELED_NETHER_BRICKS, Items.CHISELED_TUFF, Items.CHISELED_TUFF_BRICKS,
                Items.MOSSY_STONE_BRICKS, Items.MOSSY_STONE_BRICK_SLAB,
                Items.MOSSY_STONE_BRICK_STAIRS, Items.MOSSY_STONE_BRICK_WALL,
                Items.MOSSY_COBBLESTONE_SLAB, Items.MOSSY_COBBLESTONE_STAIRS, Items.MOSSY_COBBLESTONE_WALL);

        // Тир 5 — дипслейт (глубинная порода)
        gate(new SpecRequirement(Spec.CRAFTSMAN, 5),
                Items.COBBLED_DEEPSLATE_SLAB, Items.COBBLED_DEEPSLATE_STAIRS, Items.COBBLED_DEEPSLATE_WALL,
                Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE_SLAB,
                Items.POLISHED_DEEPSLATE_STAIRS, Items.POLISHED_DEEPSLATE_WALL,
                Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_BRICK_SLAB,
                Items.DEEPSLATE_BRICK_STAIRS, Items.DEEPSLATE_BRICK_WALL,
                Items.DEEPSLATE_TILES, Items.DEEPSLATE_TILE_SLAB,
                Items.DEEPSLATE_TILE_STAIRS, Items.DEEPSLATE_TILE_WALL,
                Items.CHISELED_DEEPSLATE);

        // Тир 6 — блэкстоун (Нижний) + кварц
        gate(new SpecRequirement(Spec.CRAFTSMAN, 6),
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

        // Тир 7 — призмарин (океан), пурпур и энд-камень (Край)
        gate(new SpecRequirement(Spec.CRAFTSMAN, 7),
                Items.PRISMARINE_SLAB, Items.PRISMARINE_STAIRS, Items.PRISMARINE_WALL,
                Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICK_SLAB, Items.PRISMARINE_BRICK_STAIRS,
                Items.DARK_PRISMARINE, Items.DARK_PRISMARINE_SLAB, Items.DARK_PRISMARINE_STAIRS,
                Items.PURPUR_BLOCK, Items.PURPUR_PILLAR, Items.PURPUR_SLAB, Items.PURPUR_STAIRS,
                Items.END_STONE_BRICKS, Items.END_STONE_BRICK_SLAB,
                Items.END_STONE_BRICK_STAIRS, Items.END_STONE_BRICK_WALL);
        // Стол зачарования НЕ гейтится — крафтабелен с ур.0 (профильный
        // инструмент Зачарователя, нужен для старта прокачки). XP/двойной крафт
        // привязаны к Зачарователю через ItemCraftMap.

        // ============================================================
        // АЛХИМИК — реагенты и магический инвентарь
        // ============================================================
        gate(new SpecRequirement(Spec.ALCHEMIST, 2),
                Items.FERMENTED_SPIDER_EYE, Items.GLISTERING_MELON_SLICE);

        gate(new SpecRequirement(Spec.ALCHEMIST, 3),
                Items.BREWING_STAND);

        gate(new SpecRequirement(Spec.ALCHEMIST, 4),
                Items.ENDER_EYE);

        gate(new SpecRequirement(Spec.ALCHEMIST, 5),
                Items.END_CRYSTAL);

        // ============================================================
        // ЗАЧАРОВАТЕЛЬ
        // ============================================================
        gate(new SpecRequirement(Spec.ENCHANTER, 3),
                Items.ENCHANTED_BOOK);

        // ============================================================
        // DUAL-SPEC: Алхимик 3 + Зачарователь 3
        // ============================================================
        gateAll(Items.GOLDEN_CARROT,
                new SpecRequirement(Spec.ALCHEMIST, 3), new SpecRequirement(Spec.ENCHANTER, 3));
        gateAll(Items.GOLDEN_APPLE,
                new SpecRequirement(Spec.ALCHEMIST, 3), new SpecRequirement(Spec.ENCHANTER, 3));
    }

    /** Одно требование на несколько предметов. */
    private static void gate(SpecRequirement req, Item... items) {
        for (Item item : items) MAP.put(item, List.of(req));
    }

    /** Несколько требований (все обязательны) на один предмет — dual-spec. */
    private static void gateAll(Item item, SpecRequirement... reqs) {
        MAP.put(item, List.of(reqs));
    }

    /** Список требований для крафта предмета, либо null (без ограничений). */
    public static List<SpecRequirement> get(Item item) {
        return MAP.get(item);
    }

    /** Регистрация крафт-гейта по ID (мод-совместимость). No-op если предмета нет. */
    public static void addById(String id, SpecRequirement... reqs) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, List.of(reqs)));
    }
}
