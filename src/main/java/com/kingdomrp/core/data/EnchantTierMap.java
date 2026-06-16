package com.kingdomrp.core.data;

import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

/**
 * Лестница доступа Зачарователя (path {@link Path#MAGIC}, spec {@link Spec#ENCHANTER}) —
 * гейтинг ЗАЧАРОВАНИЯ по уровню специализации. Возвращает требуемый уровень для
 * получения данного результата; гейт «не запускать» сравнивает его с уровнем
 * игрока.
 * <p>
 * Тир по редкости чары (база опущена до 0 — стол всё равно не предложит
 * недоступное благодаря фильтрации пула в {@code EnchantmentHelperMixin}, а
 * «нужда в прокачке» выражается шансом успеха и разблокировкой RARE/VERY_RARE):
 * <ul>
 *   <li>COMMON = 0</li>
 *   <li>UNCOMMON = 0</li>
 *   <li>RARE = 3</li>
 *   <li>VERY_RARE = 5</li>
 * </ul>
 * Сверх того: проклятие или чара, наложенная на МАКСИМАЛЬНЫЙ уровень (для чар с
 * maxLevel > 1) → 7. Тиры по типу действия (книга на столе = 3, наковальня = 5)
 * задаются в {@code EnchantSystem} и комбинируются с тиром редкости.
 */
public class EnchantTierMap {

    public static final int CURSE_LEVEL    = 7;
    public static final int MAXLEVEL_LEVEL = 7;

    private static int rarityTier(Enchantment.Rarity rarity) {
        return switch (rarity) {
            case COMMON    -> 0;
            case UNCOMMON  -> 0; // базовый набор доступен с ур.0, гейтится шансом
            case RARE      -> 3;
            case VERY_RARE -> 5;
        };
    }

    /** Требуемый уровень для одной чары данного уровня. */
    public static int requiredForEnchant(Enchantment enchantment, int level) {
        int req = rarityTier(enchantment.getRarity());
        if (enchantment.isCurse()) {
            req = Math.max(req, CURSE_LEVEL);
        }
        if (enchantment.getMaxLevel() > 1 && level >= enchantment.getMaxLevel()) {
            req = Math.max(req, MAXLEVEL_LEVEL);
        }
        return req;
    }

    /** Требуемый уровень для набора чар (максимум по всем). */
    public static int requiredForEnchants(Map<Enchantment, Integer> enchantments) {
        int req = 0;
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            req = Math.max(req, requiredForEnchant(e.getKey(), e.getValue()));
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
