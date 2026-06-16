package com.kingdomrp.core.data;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * Маппинг XP Зачарователя (path {@link Path#MAGIC}) за наложенные/применённые
 * чары. XP считается по редкости чары × её уровень и суммируется по всем
 * чарам результата (аналог {@link BrewXPMap} по тирам зелий).
 * <p>
 * Тиры (XP за уровень I):
 * <ul>
 *   <li>COMMON = 3 (Protection, Sharpness, Efficiency, Unbreaking)</li>
 *   <li>UNCOMMON = 5 (Fire Aspect, Knockback, Respiration)</li>
 *   <li>RARE = 9 (Fortune, Looting, Thorns)</li>
 *   <li>VERY_RARE = 16 (Silk Touch, Mending, Infinity)</li>
 * </ul>
 * XP за чару уровня N = baseXP(rarity) × N. Грубо коррелирует с ценностью.
 */
public class EnchantXPMap {

    private static float baseXP(Enchantment.Rarity rarity) {
        return switch (rarity) {
            case COMMON    -> 3f;
            case UNCOMMON  -> 5f;
            case RARE      -> 9f;
            case VERY_RARE -> 16f;
        };
    }

    /** XP за карту чар (например, разница после применения книги). */
    public static float xp(Map<Enchantment, Integer> enchantments) {
        float total = 0f;
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            total += baseXP(e.getKey().getRarity()) * e.getValue();
        }
        return total;
    }

    /** XP за все чары предмета. */
    public static float xp(ItemStack stack) {
        return xp(EnchantmentHelper.getEnchantments(stack));
    }

    /**
     * XP только за НЕ-проклятые чары предмета. Точильный камень снимает только
     * не-проклятия — XP начисляется ровно за снятое.
     */
    public static float xpNonCurse(ItemStack stack) {
        float total = 0f;
        for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            if (!e.getKey().isCurse()) {
                total += baseXP(e.getKey().getRarity()) * e.getValue();
            }
        }
        return total;
    }
}
