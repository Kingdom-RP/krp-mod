package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Гейтинг ПОСАДКИ растений по уровню Фермера (XP за посадку не выдаётся).
 * Ключ — блок, который появляется в мире при посадке (результат EntityPlaceEvent).
 *
 * GROWABLE — культуры, которые ОБЯЗАНЫ вырасти перед сбором. Их клетки НЕ
 * помечаются в PlacedBlockTracker, поэтому сбор своей грядки даёт XP и бонусы.
 * Абуза нет: посадить можно только в незрелом виде (age=0).
 * Мгновенно-собираемые растения (тростник/бамбук/кактус/грибы) в GROWABLE НЕ
 * входят — они остаются под трекером, чтобы не было place-break-place.
 */
public class PlantTierMap {

    private static final Map<Block, PlantEntry> MAP = new HashMap<>();
    private static final Set<Block> GROWABLE = new HashSet<>();

    static {
        // Ур.0 — стартовые культуры
        plant(0, Blocks.WHEAT, Blocks.SWEET_BERRY_BUSH);

        // Ур.1 — корнеплоды, бамбук, грибы
        plant(1, Blocks.CARROTS, Blocks.POTATOES,
                Blocks.BAMBOO_SAPLING, Blocks.BAMBOO,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM);

        // Ур.2 — свёкла, тростник, кактус
        plant(2, Blocks.BEETROOTS, Blocks.SUGAR_CANE, Blocks.CACTUS);

        // Ур.3 — крупные плоды (стебли)
        plant(3, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM);

        // Ур.4 — какао
        plant(4, Blocks.COCOA);

        // Ур.5 — нижний варт
        plant(5, Blocks.NETHER_WART);

        // Ур.6 — светящиеся ягоды, грибы Нижнего
        plant(6, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
                Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);

        // Ур.7 — торчфлауэр
        plant(7, Blocks.TORCHFLOWER_CROP);

        // Ур.8 — pitcher plant
        plant(8, Blocks.PITCHER_CROP);

        // Растущие культуры — исключаются из PlacedBlockTracker
        growable(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                Blocks.NETHER_WART, Blocks.COCOA, Blocks.SWEET_BERRY_BUSH,
                Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
                Blocks.TORCHFLOWER_CROP, Blocks.PITCHER_CROP);
    }

    private static void plant(int level, Block... blocks) {
        for (Block b : blocks) {
            MAP.put(b, new PlantEntry(Spec.FARMER, level));
        }
    }

    private static void growable(Block... blocks) {
        Collections.addAll(GROWABLE, blocks);
    }

    public static PlantEntry get(Block block) {
        return MAP.get(block);
    }

    public static boolean isGrowable(Block block) {
        return GROWABLE.contains(block);
    }

    /** Регистрация культуры по ID (мод-совместимость, напр. Farmer's Delight). No-op если блока нет. */
    public static void addById(String id, int level, boolean growableFlag) {
        net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(b -> {
                    MAP.put(b, new PlantEntry(Spec.FARMER, level));
                    if (growableFlag) GROWABLE.add(b);
                });
    }
}
