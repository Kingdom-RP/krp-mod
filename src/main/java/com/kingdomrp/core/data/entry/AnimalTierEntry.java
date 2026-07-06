package com.kingdomrp.core.data.entry;

import com.kingdomrp.core.data.type.*;

/**
 * Требование уровня специализации для разведения животного + XP за разведение.
 * Ключ в {@link AnimalTierMap} — {@code EntityType} животного.
 * Тот же уровень используется для гейтинга доения/стрижки.
 */
public record AnimalTierEntry(Spec spec, int level, float breedXP) {
}
