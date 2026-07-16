package com.kingdomrp.core.compat;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.world.entity.player.Player;

/**
 * Интеграция с модом Tiered More (рефорж тиров). Смещение шанса на лучший тир: в
 * {@code ModifierUtils.applyDynamicWeightModifiers} / {@code getRandomAttributeIDFor}
 * Tiered уменьшает самые «частые» веса на {@code luckReforgeModifier × player.getLuck()}.
 * Через {@code ReforgeLuckMixin} ПОДМЕНЯЕМ {@code getLuck()} на {@link #reforgeLuck} =
 * вся удача игрока (зелья, маяк, бафф королевства — ванильный {@code Attributes.LUCK})
 * ПЛЮС бонус за уровень Мастерового. Всё per-player (игрок — параметр метода), без
 * записи в конфиг Tiered и без модификации ванильного Luck-атрибута.
 * <p>
 * ⚠️ Зависит только от ДЕФОЛТНОГО {@code luckReforgeModifier > 0} в конфиге Tiered
 * More (по умолчанию 0.02) — своих правок конфига не требует.
 */
public final class TieredMoreCompat {

    public static final String MODID = "tiered_more";
    private static final float LUCK_PER_LEVEL = 1.0f;

    private TieredMoreCompat() {}

    /** Удача рефоржа = вся удача игрока + уровень Мастерового × {@link #LUCK_PER_LEVEL}. */
    public static float reforgeLuck(Player player) {
        if (player == null) return 0f;
        int level = player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.CRAFTSMAN.id);
        return player.getLuck() + Math.max(0, level) * LUCK_PER_LEVEL;
    }
}
