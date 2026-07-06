package com.kingdomrp.core.data.entry;

import com.kingdomrp.core.data.type.*;

/**
 * Требование уровня специализации для ПОСАДКИ растения. Ключ в
 * {@link PlantTierMap} — поставленный игроком блок (результат посадки).
 * XP за посадку не выдаётся.
 */
public record PlantEntry(Spec spec, int level) {
}
