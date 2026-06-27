package com.kingdomrp.core.data;

/**
 * Требование уровня специализации для ПОСАДКИ растения. Ключ в
 * {@link PlantTierMap} — поставленный игроком блок (результат посадки).
 * XP за посадку не выдаётся.
 */
public record PlantEntry(Spec spec, int level) {
}
