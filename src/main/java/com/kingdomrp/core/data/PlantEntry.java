package com.kingdomrp.core.data;

/**
 * Требование уровня специализации для ПОСАДКИ растения + XP за посадку.
 * Ключ в {@link PlantTierMap} — поставленный игроком блок (результат посадки).
 */
public record PlantEntry(Spec spec, int level, float plantXP) {
}
