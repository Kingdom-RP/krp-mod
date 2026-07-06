package com.kingdomrp.core.data.map.tier;

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
 * Гейтинг ПЕРЕПЛАВКИ по уровню специализации. Ключ — РЕЗУЛЬТАТ выплавки; гейт вешается
 * на вход печи ({@code CookGatedInputSlot.mayPlace} через {@link RestrictionSystem},
 * spec берётся из {@link SpecRequirement} — не хардкод). Металл — Кузнец; стекло —
 * Мастеровой ур.1 (песок ваниль+моды → стекло). Прочие натуральные материалы
 * (булыжник/камень/глина) НЕ гейтятся — только XP ({@link NaturalSmeltMap}). Еда —
 * отдельно Повар ({@code FoodTierMap}). Дедлока нет: уровень добывается любой
 * активностью соответствующего пути.
 */
public class SmeltTierMap {

    private static final Map<Item, SpecRequirement> MAP = new HashMap<>();

    static {
        initCraftPath();
    }

    // Путь "Ремесло"
    private static void initCraftPath() {
        initBlacksmith();
        initCraftsman();
    }

    // Специализация "Кузнец" (металл)
    private static void initBlacksmith() {
        // Медь — ур.1
        reg(1, Items.COPPER_INGOT);

        // Железо и золото — ур.2 (+ переплавка инструментов/брони в самородки)
        reg(2, Items.IRON_INGOT, Items.GOLD_INGOT,
                Items.IRON_NUGGET, Items.GOLD_NUGGET);

        // Незеритовый лом (древние обломки) — ур.5
        reg(5, Items.NETHERITE_SCRAP);
    }

    // Специализация "Мастеровой" (стекло)
    private static void initCraftsman() {
        // Стекло — ур.1 (любой песок: ваниль + моды → minecraft:glass)
        MAP.put(Items.GLASS, new SpecRequirement(Spec.CRAFTSMAN, 1));
    }

    private static void reg(int level, Item... results) {
        for (Item item : results) MAP.put(item, new SpecRequirement(Spec.BLACKSMITH, level));
    }

    /** Требование уровня Кузнеца для переплавки в этот результат, либо null. */
    public static SpecRequirement get(Item resultItem) {
        return MAP.get(resultItem);
    }
}
