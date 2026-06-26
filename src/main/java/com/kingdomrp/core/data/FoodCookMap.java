package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Маппинг опыта за ГОТОВКУ (термообработку) еды для пути Промысел
 * (специализация Повар). Ключ — результат готовки в печи/коптильне/костре.
 * XP начисляется за штуку готового продукта (× количество при изъятии).
 * <p>
 * Тиры по пищевой ценности: базовое (1) &lt; рыба (2) &lt; мясо (3) &lt;
 * жирное мясо (4). XP за готовку намеренно ниже, чем за крафт составных
 * блюд (см. {@link ItemCraftMap}) — готовка масштабируется количеством.
 */
public class FoodCookMap {

    private static final Map<Item, Float> MAP = new HashMap<>();

    static {
        // Базовое
        register(1f, Items.DRIED_KELP, Items.BAKED_POTATO);

        // Рыба
        register(2f, Items.COOKED_COD, Items.COOKED_SALMON);

        // Мясо
        register(3f, Items.COOKED_CHICKEN, Items.COOKED_RABBIT, Items.COOKED_MUTTON);

        // Жирное мясо
        register(4f, Items.COOKED_BEEF, Items.COOKED_PORKCHOP);
    }

    private static void register(float xp, Item... items) {
        for (Item item : items) {
            MAP.put(item, xp);
        }
    }

    /** XP за готовку одной штуки продукта, либо 0, если предмет не относится к Повару. */
    public static float get(Item item) {
        return MAP.getOrDefault(item, 0f);
    }

    /** Регистрация XP за готовку по ID (мод-совместимость). No-op если предмета нет. */
    public static void addById(String id, float xp) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, xp));
    }
}
