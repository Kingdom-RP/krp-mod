package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

// Гейтинг ДОБЫЧИ блока (руда/дерево/урожай) по уровню специализации
public class BlockTierMap {

    private static final Map<Block, BlockTierEntry> MAP = new HashMap<>();

    static {
        initMiningPath();
        initHarvestPath();
    }

    // Путь "Добыча"
    private static void initMiningPath() {
        initMiner();
        initLumberjack();
    }

    // Специализация "Шахтёр"
    private static void initMiner() {
        // Шахтёр — уровень 1: уголь, медь
        register(new BlockTierEntry(Spec.MINER, 1),
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);

        // Шахтёр — уровень 2: железо, золото
        register(new BlockTierEntry(Spec.MINER, 2),
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);

        // Шахтёр — уровень 3: редстоун, лазурит, изумруд
        register(new BlockTierEntry(Spec.MINER, 3),
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);

        // Шахтёр — уровень 4: алмазы, обсидиан
        register(new BlockTierEntry(Spec.MINER, 4),
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);

        // Шахтёр — уровень 5: незер
        register(new BlockTierEntry(Spec.MINER, 5),
                Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
                Blocks.ANCIENT_DEBRIS);
    }

    // Специализация "Лесоруб"
    private static void initLumberjack() {
        // ВСЕ оверворлд-деревья доступны с ур.0 (гейта нет) — иначе игрок в биоме
        // с одним видом дерева не добыл бы древесину. XP всё равно идёт (BlockXPMap).
        // Ур.6: незер-стебли и гигантские грибы — гейт сохранён (нужен поход в Нижний).
        register(new BlockTierEntry(Spec.LUMBERJACK, 6),
                Blocks.CRIMSON_STEM, Blocks.WARPED_STEM,
                Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
                Blocks.MUSHROOM_STEM);
    }

    // Путь "Промысел"
    private static void initHarvestPath() {
        initFarmer();
    }

    // Специализация "Фермер" (ур.0 — пшеница и сладкие ягоды — без гейта)
    private static void initFarmer() {
        // Ур.1: морковь, картофель, бамбук, грибы
        register(new BlockTierEntry(Spec.FARMER, 1),
                Blocks.CARROTS, Blocks.POTATOES, Blocks.BAMBOO,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM);

        // Ур.2: свёкла, тростник, кактус
        register(new BlockTierEntry(Spec.FARMER, 2),
                Blocks.BEETROOTS, Blocks.SUGAR_CANE, Blocks.CACTUS);

        // Ур.3: тыква, арбуз
        register(new BlockTierEntry(Spec.FARMER, 3),
                Blocks.PUMPKIN, Blocks.MELON);

        // Ур.4: какао
        register(new BlockTierEntry(Spec.FARMER, 4),
                Blocks.COCOA);

        // Ур.5: нижний варт
        register(new BlockTierEntry(Spec.FARMER, 5),
                Blocks.NETHER_WART);

        // Ур.6: светящиеся ягоды, грибы Нижнего
        register(new BlockTierEntry(Spec.FARMER, 6),
                Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
                Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);

        // Ур.7: торчфлауэр
        register(new BlockTierEntry(Spec.FARMER, 7),
                Blocks.TORCHFLOWER);

        // Ур.8: pitcher plant
        register(new BlockTierEntry(Spec.FARMER, 8),
                Blocks.PITCHER_PLANT);
    }

    private static void register(BlockTierEntry entry, Block... blocks) {
        for (Block block : blocks) {
            MAP.put(block, entry);
        }
    }

    public static BlockTierEntry get(Block block) {
        return MAP.get(block);
    }

    /** Регистрация блока по ID (мод-совместимость, напр. Farmer's Delight). No-op если блок отсутствует. */
    public static void addById(String id, Spec spec, int level) {
        BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id))
                .ifPresent(b -> MAP.put(b, new BlockTierEntry(spec, level)));
    }
}