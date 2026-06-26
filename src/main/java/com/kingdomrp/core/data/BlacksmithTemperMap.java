package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Закалка Кузнеца: для каждого металлического изделия — уровень открытия тира
 * ({@code unlock}) и уровень полной прочности ({@code full}). Прочность
 * свежескованного предмета растёт линейно от 50% на {@code unlock} до 100% на
 * {@code full}.
 * Низкоуровневый Кузнец делает рабочий, но менее долговечный предмет. Применяется
 * на крафте ({@code SpecializationEffects.applyBlacksmithTempering}) и незерит-
 * апгрейде ({@code SmithingMenuMixin}).
 */
public class BlacksmithTemperMap {

    public record TemperTier(int unlock, int full) {}

    private static final Map<Item, TemperTier> MAP = new HashMap<>();

    static {
        // Камень — нижний тир инструментов: открытие ур.1, полная прочность ур.4.
        // Камень не натуральный материал Мастерового и не металл Кузнеца в чистом
        // виде, но обработка камня в инструмент ближе к кузнечному делу.
        reg(new TemperTier(1, 4),
                Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_PICKAXE,
                Items.STONE_SHOVEL, Items.STONE_HOE);

        // Золото — нижний металлический тир: открытие ур.2, полная прочность ур.3
        reg(new TemperTier(2, 3),
                Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_PICKAXE,
                Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);

        // Железо: открытие ур.3, полная прочность ур.5
        reg(new TemperTier(3, 5),
                Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE,
                Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS, Items.IRON_BOOTS);

        // Алмаз: открытие ур.5, полная прочность ур.7
        reg(new TemperTier(5, 7),
                Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);

        // Незерит: открытие ур.7, полная прочность ур.10 (апгрейд на кузнечном столе)
        reg(new TemperTier(7, 10),
                Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_PICKAXE,
                Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
    }

    private static void reg(TemperTier tier, Item... items) {
        for (Item item : items) MAP.put(item, tier);
    }

    /** Тир закалки предмета, либо null (предмет не закаляется Кузнецом). */
    public static TemperTier get(Item item) {
        return MAP.get(item);
    }

    /** Доля прочности (0.5..1.0) для свежего изделия при данном уровне Кузнеца. */
    public static float quality(TemperTier tier, int level) {
        if (tier == null) return 1.0f;
        if (level >= tier.full()) return 1.0f;
        if (level <= tier.unlock()) return 0.5f;
        float frac = (float) (level - tier.unlock()) / (tier.full() - tier.unlock());
        return 0.5f + 0.5f * frac;
    }
}
