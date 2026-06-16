package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Гейтинг ПЕРЕПЛАВКИ металла по уровню Кузнеца. Ключ — РЕЗУЛЬТАТ выплавки
 * (как в {@link MetalSmeltMap}); гейт вешается на вход печи
 * ({@code CookGatedInputSlot.mayPlace} через {@link RestrictionSystem}).
 * <p>
 * Только металл. Песок/булыжник/глина и прочие натуральные материалы НЕ гейтятся
 * вовсе — их обжиг закреплён за Мастеровым ({@link NaturalSmeltMap}, только XP,
 * без гейта: стекло/камень нужны всем и рано). Еда гейтится отдельно Поваром
 * ({@code FoodTierMap}). Дедлока нет: уровень Кузнеца добывается любой активностью
 * пути Ремесло (например, Плотником) → очки в Кузнеца.
 */
public class SmeltTierMap {

    private static final Map<Item, SpecRequirement> MAP = new HashMap<>();

    static {
        // Медь — ур.1
        reg(1, Items.COPPER_INGOT);

        // Железо и золото — ур.2 (+ переплавка инструментов/брони в самородки)
        reg(2, Items.IRON_INGOT, Items.GOLD_INGOT,
                Items.IRON_NUGGET, Items.GOLD_NUGGET);

        // Незеритовый лом (древние обломки) — ур.5
        reg(5, Items.NETHERITE_SCRAP);
    }

    private static void reg(int level, Item... results) {
        for (Item item : results) MAP.put(item, new SpecRequirement(Spec.BLACKSMITH, level));
    }

    /** Требование уровня Кузнеца для переплавки в этот результат, либо null. */
    public static SpecRequirement get(Item resultItem) {
        return MAP.get(resultItem);
    }
}
