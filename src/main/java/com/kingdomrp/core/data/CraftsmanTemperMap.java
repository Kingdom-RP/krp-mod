package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Закалка Мастерового — аналог {@link BlacksmithTemperMap}, но для изделий из
 * натуральных материалов (сейчас — кожаная броня). Для каждого предмета задаётся
 * уровень открытия тира ({@code unlock}) и уровень полной прочности ({@code full}):
 * прочность свежесделанного предмета растёт линейно от 50% на {@code unlock} до
 * 100% на {@code full}.
 * <p>
 * Применяется только на крафте (верстак, см.
 * {@code SpecializationEffects.applyCraftsmanTempering}). Карта расширяема: новые
 * типы брони из модов (на натуральных материалах) добавляются сюда же со своими
 * тирами.
 */
public class CraftsmanTemperMap {

    private static final Map<Item, BlacksmithTemperMap.TemperTier> MAP = new HashMap<>();

    static {
        // Кожаная броня. Лёгкие части (ботинки/шлем) открываются раньше тяжёлых
        // (куртка/штаны) — совпадает с лестницей доступа крафта (ур.1 / ур.2).
        reg(new BlacksmithTemperMap.TemperTier(1, 6),
                Items.LEATHER_BOOTS, Items.LEATHER_HELMET);

        reg(new BlacksmithTemperMap.TemperTier(2, 8),
                Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS);
    }

    private static void reg(BlacksmithTemperMap.TemperTier tier, Item... items) {
        for (Item item : items) MAP.put(item, tier);
    }

    /** Тир закалки предмета, либо null (не закаляется Мастеровым). */
    public static BlacksmithTemperMap.TemperTier get(Item item) {
        return MAP.get(item);
    }
}
