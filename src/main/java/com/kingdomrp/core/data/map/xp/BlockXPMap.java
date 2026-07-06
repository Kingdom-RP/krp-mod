package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Маппинг получения опыта за разрушение блоков соответствующих путей/специализаций
public class BlockXPMap {

    private static final Map<Block, BlockEntry> MAP = new HashMap<>();
    /** Тег-правила (проверяются после точных Block, для мод-совместимости). */
    private static final List<Map.Entry<TagKey<Block>, BlockEntry>> TAGS = new ArrayList<>();

    static {
        initMiningPath();
        initHarvestPath();
        initMagicPath();
    }

    // Путь "Добыча"
    private static void initMiningPath() {
        initMiner();
        initLumberjack();
    }

    // Специализация "Шахтёр"
    private static void initMiner() {
        // Камень обычный (якорь = 1)
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.STONE, Blocks.COBBLESTONE, Blocks.SMOOTH_STONE);

        // Декоративный камень
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE);

        // Специальный камень
        register(new BlockEntry(Path.MINING, 1.5f),
                Blocks.TUFF, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK);

        // Deepslate (твёрже в 2 раза)
        register(new BlockEntry(Path.MINING, 2f),
                Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);

        // Песок и гравий
        register(new BlockEntry(Path.MINING, 0.5f),
                Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL);

        // Глина
        register(new BlockEntry(Path.MINING, 0.5f),
                Blocks.CLAY);

        // Терракота
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.TERRACOTTA);

        // Лёд
        register(new BlockEntry(Path.MINING, 0.5f),
                Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);

        // Обсидиан
        register(new BlockEntry(Path.MINING, 8f),
                Blocks.OBSIDIAN);
        register(new BlockEntry(Path.MINING, 10f),
                Blocks.CRYING_OBSIDIAN);

        // Руды обычного мира
        register(new BlockEntry(Path.MINING, 5f),
                Blocks.COAL_ORE);
        register(new BlockEntry(Path.MINING, 7f),
                Blocks.DEEPSLATE_COAL_ORE);

        register(new BlockEntry(Path.MINING, 10f),
                Blocks.IRON_ORE);
        register(new BlockEntry(Path.MINING, 15f),
                Blocks.DEEPSLATE_IRON_ORE);

        register(new BlockEntry(Path.MINING, 10f),
                Blocks.COPPER_ORE);
        register(new BlockEntry(Path.MINING, 15f),
                Blocks.DEEPSLATE_COPPER_ORE);

        register(new BlockEntry(Path.MINING, 12f),
                Blocks.GOLD_ORE);
        register(new BlockEntry(Path.MINING, 18f),
                Blocks.DEEPSLATE_GOLD_ORE);

        register(new BlockEntry(Path.MINING, 12f),
                Blocks.LAPIS_ORE);
        register(new BlockEntry(Path.MINING, 18f),
                Blocks.DEEPSLATE_LAPIS_ORE);

        register(new BlockEntry(Path.MINING, 12f),
                Blocks.REDSTONE_ORE);
        register(new BlockEntry(Path.MINING, 18f),
                Blocks.DEEPSLATE_REDSTONE_ORE);

        register(new BlockEntry(Path.MINING, 18f),
                Blocks.EMERALD_ORE);
        register(new BlockEntry(Path.MINING, 27f),
                Blocks.DEEPSLATE_EMERALD_ORE);

        register(new BlockEntry(Path.MINING, 25f),
                Blocks.DIAMOND_ORE);
        register(new BlockEntry(Path.MINING, 37f),
                Blocks.DEEPSLATE_DIAMOND_ORE);

        // Нижний мир — руды
        register(new BlockEntry(Path.MINING, 8f),
                Blocks.NETHER_QUARTZ_ORE);
        register(new BlockEntry(Path.MINING, 10f),
                Blocks.NETHER_GOLD_ORE);
        register(new BlockEntry(Path.MINING, 50f),
                Blocks.ANCIENT_DEBRIS);

        // Нижний мир — камень
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.NETHERRACK);
        register(new BlockEntry(Path.MINING, 1.5f),
                Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE);
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.SOUL_SAND, Blocks.SOUL_SOIL);
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM);

        // Край
        register(new BlockEntry(Path.MINING, 3f),
                Blocks.END_STONE);

        // Специальные блоки
        register(new BlockEntry(Path.MINING, 5f),
                Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS,
                Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN);
        register(new BlockEntry(Path.MINING, 3f),
                Blocks.MAGMA_BLOCK);
        register(new BlockEntry(Path.MINING, 10f),
                Blocks.SCULK, Blocks.SCULK_SENSOR,
                Blocks.SCULK_CATALYST, Blocks.SCULK_SHRIEKER);
        register(new BlockEntry(Path.MINING, 4f),
                Blocks.AMETHYST_CLUSTER, Blocks.BUDDING_AMETHYST);
        register(new BlockEntry(Path.MINING, 2f),
                Blocks.POINTED_DRIPSTONE);
        register(new BlockEntry(Path.MINING, 3f),
                Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS, Blocks.PACKED_MUD);
        register(new BlockEntry(Path.MINING, 5f),
                Blocks.SPONGE, Blocks.WET_SPONGE);
        register(new BlockEntry(Path.MINING, 3f),
                Blocks.MOSS_BLOCK);
        register(new BlockEntry(Path.MINING, 2f),
                Blocks.ROOTED_DIRT, Blocks.DIRT,
                Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM);
        register(new BlockEntry(Path.MINING, 1f),
                Blocks.BONE_BLOCK);
    }

    // Специализация "Лесоруб"
    private static void initLumberjack() {
        // Уровень 0 — мягкие породы (дуб, берёза, ель)
        register(new BlockEntry(Path.MINING, 2f),
                Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG);

        // Уровень 2 — джунгли, акация, тёмный дуб
        register(new BlockEntry(Path.MINING, 4f),
                Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG);

        // Уровень 4 — мангровое, вишня
        register(new BlockEntry(Path.MINING, 8f),
                Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG);

        // Уровень 6 — незер, грибы
        register(new BlockEntry(Path.MINING, 16f),
                Blocks.CRIMSON_STEM, Blocks.WARPED_STEM,
                Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
                Blocks.MUSHROOM_STEM);

        // Корни (бамбук — у Фермера, путь Промысел)
        register(new BlockEntry(Path.MINING, 0.5f),
                Blocks.MANGROVE_ROOTS);
    }

    // Путь "Промысел"
    private static void initHarvestPath() {
        initFarmer();
        initFisher();
    }

    // Специализация "Фермер" (XP коррелирует с уровнем доступа — BlockTierMap)
    private static void initFarmer() {
        // Ур.0 — стартовые культуры
        // (XP повышен после отмены XP за посадку — награда только за сбор созревшего)
        register(new BlockEntry(Path.HARVEST, 3f),
                Blocks.WHEAT, Blocks.SWEET_BERRY_BUSH);

        // Ур.1 — корнеплоды, бамбук
        register(new BlockEntry(Path.HARVEST, 4f),
                Blocks.CARROTS, Blocks.POTATOES);
        register(new BlockEntry(Path.HARVEST, 1f),
                Blocks.BAMBOO);

        // Ур.2 — свёкла, тростник, кактус
        register(new BlockEntry(Path.HARVEST, 4f),
                Blocks.BEETROOTS);
        register(new BlockEntry(Path.HARVEST, 2f),
                Blocks.SUGAR_CANE, Blocks.CACTUS);

        // Ур.3 — крупные плоды, грибы
        register(new BlockEntry(Path.HARVEST, 5f),
                Blocks.PUMPKIN, Blocks.MELON);
        register(new BlockEntry(Path.HARVEST, 3f),
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM);

        // Ур.4 — какао
        register(new BlockEntry(Path.HARVEST, 5f),
                Blocks.COCOA);

        // Ур.5 — нижний варт
        register(new BlockEntry(Path.HARVEST, 7f),
                Blocks.NETHER_WART);

        // Ур.6 — светящиеся ягоды, грибы Нижнего
        register(new BlockEntry(Path.HARVEST, 5f),
                Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
        register(new BlockEntry(Path.HARVEST, 4f),
                Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);

        // Ур.7 — торчфлауэр
        register(new BlockEntry(Path.HARVEST, 10f),
                Blocks.TORCHFLOWER);

        // Ур.8 — pitcher plant
        register(new BlockEntry(Path.HARVEST, 12f),
                Blocks.PITCHER_PLANT);

        // Декоративка — не гейтится по уровню (свободна с ур.0)
        // Листва
        register(new BlockEntry(Path.HARVEST, 0.5f),
                Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES,
                Blocks.SPRUCE_LEAVES, Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES,
                Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES);

        // Декоративная флора без alchemy-ценности — остаётся у Промысла
        register(new BlockEntry(Path.HARVEST, 0.5f),
                Blocks.PINK_PETALS, Blocks.SPORE_BLOSSOM);

        // Лоза и ползучие растения
        register(new BlockEntry(Path.HARVEST, 0.5f),
                Blocks.VINE, Blocks.WEEPING_VINES,
                Blocks.TWISTING_VINES,
                Blocks.BIG_DRIPLEAF, Blocks.SMALL_DRIPLEAF);
    }

    // Специализация "Рыбак" (натуральная морская флора)
    private static void initFisher() {
        // Водоросли и морские растения
        register(new BlockEntry(Path.HARVEST, 1f),
                Blocks.KELP, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS);

        // Водяные лилии
        register(new BlockEntry(Path.HARVEST, 1f),
                Blocks.LILY_PAD);

        // Морские огурцы
        register(new BlockEntry(Path.HARVEST, 1f),
                Blocks.SEA_PICKLE);
    }

    // Путь "Магия"
    private static void initMagicPath() {
        initAlchemist();
    }

    // Специализация "Алхимик" (сбор цветов/реагентного сырья)
    private static void initAlchemist() {
        // Мелкие цветы (сырьё для красителей/зелий) — по тегу: ваниль + моды
        // (BWG/BOP дописывают свои цветы в minecraft:small_flowers/tall_flowers)
        registerTag(BlockTags.SMALL_FLOWERS, new BlockEntry(Path.MAGIC, 0.5f));

        // Высокие цветы (двойной блок — больше сырья)
        registerTag(BlockTags.TALL_FLOWERS, new BlockEntry(Path.MAGIC, 1f));

        // Глоустоун (натуральный)
        register(new BlockEntry(Path.MAGIC, 2f),
                Blocks.GLOWSTONE);
    }

    private static void register(BlockEntry entry, Block... blocks) {
        for (Block block : blocks) {
            MAP.put(block, entry);
        }
    }

    private static void registerTag(TagKey<Block> tag, BlockEntry entry) {
        TAGS.add(Map.entry(tag, entry));
    }

    public static BlockEntry get(Block block) {
        BlockEntry direct = MAP.get(block);
        if (direct != null) return direct;
        for (var e : TAGS) {
            if (block.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        return null;
    }

    /** Регистрация блока по ID (мод-совместимость). No-op если блок отсутствует. */
    public static void addById(String id, Path path, float xp) {
        net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(b -> MAP.put(b, new BlockEntry(path, xp)));
    }
}