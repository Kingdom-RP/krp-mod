package com.kingdomrp.core.data;

import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Маппинг XP Зачарователя (path {@link Path#MAGIC}) за наложенные/применённые чары.
 * XP = baseXP(вес чары) × её уровень, сумма по всем чарам результата.
 * <p>
 * XP по ВЕСУ чары ({@code definition().weight()}):
 * <ul>
 *   <li>вес ≥10 (COMMON) = 3</li>
 *   <li>вес ≥5 (UNCOMMON) = 5</li>
 *   <li>вес ≥2 (RARE) = 9</li>
 *   <li>вес 1 (VERY_RARE) = 16</li>
 * </ul>
 */
public class EnchantXPMap {

    private static float baseXP(int weight) {
        if (weight >= 10) return 3f;
        if (weight >= 5)  return 5f;
        if (weight >= 2)  return 9f;
        return 16f;
    }

    /** XP за набор чар (например, разница после применения книги). */
    public static float xp(ItemEnchantments enchantments) {
        float total = 0f;
        for (var e : enchantments.entrySet()) {
            total += baseXP(e.getKey().value().getWeight()) * e.getIntValue();
        }
        return total;
    }

    /** XP за все чары предмета (для книг — stored enchantments). */
    public static float xp(ItemStack stack) {
        return xp(EnchantmentHelper.getEnchantmentsForCrafting(stack));
    }

    /**
     * XP только за НЕ-проклятые чары предмета. Точильный камень снимает только
     * не-проклятия — XP начисляется ровно за снятое.
     */
    public static float xpNonCurse(ItemStack stack) {
        float total = 0f;
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            if (!e.getKey().is(EnchantmentTags.CURSE)) {
                total += baseXP(e.getKey().value().getWeight()) * e.getIntValue();
            }
        }
        return total;
    }
}
