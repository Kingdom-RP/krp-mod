package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * XP Мастерового (path {@link Path#CRAFT}) за обжиг натуральных материалов в печи —
 * песок→стекло, булыжник→камень, глина→терракота и т.п. Ключ — РЕЗУЛЬТАТ обжига;
 * XP начисляется за штуку × количество при изъятии из печи
 * ({@code FurnaceResultSlotMixin} → {@link com.kingdomrp.core.system.XPSystem#onNaturalSmelted}).
 * <p>
 * Обжиг натуральных материалов НЕ гейтится уровнем (стекло/камень нужны всем и
 * рано) — даём только XP. Это та ниша, которую Кузнец явно «не трогает»
 * (металл — {@link MetalSmeltMap}/{@link SmeltTierMap}), а Повар закрывает едой.
 */
public class NaturalSmeltMap {

    private static final Map<Item, Float> MAP = new HashMap<>();

    static {
        // Стекло, камень и каменные «гладкие/трескающиеся» варианты — XP 0.5
        put(0.5f,
                Items.GLASS, Items.STONE, Items.SMOOTH_STONE,
                Items.DEEPSLATE, Items.SMOOTH_BASALT,
                Items.SMOOTH_SANDSTONE, Items.SMOOTH_RED_SANDSTONE, Items.SMOOTH_QUARTZ,
                Items.CRACKED_STONE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS,
                Items.CRACKED_NETHER_BRICKS, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);

        // Обжиг глины/землистого сырья — XP 1
        put(1f,
                Items.BRICK, Items.NETHER_BRICK, Items.TERRACOTTA);

        // Глазурованная терракота (из крашеной терракоты) — XP 1
        put(1f,
                Items.WHITE_GLAZED_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA,
                Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA,
                Items.PINK_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA,
                Items.CYAN_GLAZED_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA,
                Items.BROWN_GLAZED_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA,
                Items.BLACK_GLAZED_TERRACOTTA);

        // Сушка губки — XP 2
        put(2f, Items.SPONGE);
    }

    private static void put(float xp, Item... results) {
        for (Item item : results) MAP.put(item, xp);
    }

    /** XP за единицу обожжённого натурального материала, либо 0, если не относится к Мастеровому. */
    public static float get(Item item) {
        return MAP.getOrDefault(item, 0f);
    }
}
