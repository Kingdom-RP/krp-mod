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
 * <p>
 * Это замена шансу провала крафта: вместо «сгорело/не сгорело» низкоуровневый
 * Кузнец делает рабочий, но менее долговечный предмет. Применяется на крафте
 * (верстак, см. {@code SpecializationEffects.applyBlacksmithTempering}) и на
 * незерит-апгрейде (кузнечный стол, см. {@code SmithingMenuMixin}).
 */
public class BlacksmithTemperMap {

    public record TemperTier(int unlock, int full) {}

    private static final Map<Item, TemperTier> MAP = new HashMap<>();

    static {
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
