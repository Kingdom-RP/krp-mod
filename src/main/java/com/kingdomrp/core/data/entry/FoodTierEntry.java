package com.kingdomrp.core.data.entry;

import com.kingdomrp.core.data.type.*;

/**
 * Требование уровня специализации для производства еды (крафт ИЛИ готовка).
 * Ключ в {@link FoodTierMap} — производимый предмет.
 * Уровни выведены из пищевой ценности (голод + фактическое насыщение).
 */
public record FoodTierEntry(Spec spec, int level) {}
