package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Лестница доступа Зачарователя (path {@link Path#MAGIC}, spec {@link Spec#ENCHANTER}) —
 * гейтинг ЗАЧАРОВАНИЯ по уровню специализации. Возвращает требуемый уровень для
 * получения данного результата; гейт «не запускать» сравнивает его с уровнем игрока.
 * <p>
 * Тир по ВЕСУ чары ({@code definition().weight()}): вес 10/5/2/1 ≈ бакеты
 * COMMON/UNCOMMON/RARE/VERY_RARE. Проклятия — тег {@link EnchantmentTags#CURSE}.
 * <ul>
 *   <li>вес ≥5 (COMMON/UNCOMMON) → 0</li>
 *   <li>вес ≥2 (RARE) → 3</li>
 *   <li>вес 1 (VERY_RARE) → 5</li>
 * </ul>
 * Сверх того: проклятие или чара на МАКСИМАЛЬНОМ уровне (maxLevel > 1) → 7.
 */
public class EnchantTierMap {

    public static final int CURSE_LEVEL    = 7;
    public static final int MAXLEVEL_LEVEL = 7;

    private static int weightTier(int weight) {
        if (weight >= 5) return 0; // COMMON/UNCOMMON — доступны с ур.0, гейтятся шансом
        if (weight >= 2) return 3; // RARE
        return 5;                  // VERY_RARE (вес 1)
    }

    /** Требуемый уровень для одной чары данного уровня. */
    public static int requiredForEnchant(Holder<Enchantment> ench, int level) {
        int req = weightTier(ench.value().getWeight());
        if (ench.is(EnchantmentTags.CURSE)) {
            req = Math.max(req, CURSE_LEVEL);
        }
        int maxLevel = ench.value().getMaxLevel();
        if (maxLevel > 1 && level >= maxLevel) {
            req = Math.max(req, MAXLEVEL_LEVEL);
        }
        return req;
    }

    /** Требуемый уровень для набора чар (максимум по всем). */
    public static int requiredForEnchants(ItemEnchantments enchantments) {
        int req = 0;
        for (var e : enchantments.entrySet()) {
            req = Math.max(req, requiredForEnchant(e.getKey(), e.getIntValue()));
        }
        return req;
    }

    /**
     * Сколько вариантов зачарования на столе доступно на данном уровне:
     * ур.0–2 → 1 (только верхний, самый слабый), ур.3–4 → 2, ур.5+ → 3.
     */
    public static int slotCount(int enchanterLevel) {
        if (enchanterLevel < 3) return 1;
        if (enchanterLevel < 5) return 2;
        return 3;
    }
}
