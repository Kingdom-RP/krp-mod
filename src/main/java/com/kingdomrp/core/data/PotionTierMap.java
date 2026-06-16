package com.kingdomrp.core.data;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Лестница доступа Алхимика (path {@link Path#MAGIC}, spec {@link Spec#ALCHEMIST}) —
 * гейтинг ВАРКИ по уровню специализации. Возвращает требуемый уровень для
 * получения данного результата; гейт «не запускать» сравнивает его с уровнем
 * владельца стойки.
 * <p>
 * Требование = максимум из:
 * <ul>
 *   <li>базового уровня эффекта (ключ — {@link MobEffect});</li>
 *   <li>модификатора long (Redstone) → ур.5;</li>
 *   <li>модификатора strong (Glowstone) → ур.6;</li>
 *   <li>splash-зелья (Gunpowder) → ур.7;</li>
 *   <li>lingering-зелья (Dragon's Breath) → ур.9.</li>
 * </ul>
 * Turtle Master (resistance+slowness) гейтится по resistance=ур.6.
 * База (awkward/mundane/thick — без эффектов) → ур.0 (без ограничений).
 */
public class PotionTierMap {

    private static final int LONG_LEVEL    = 5;
    private static final int STRONG_LEVEL  = 6;
    private static final int SPLASH_LEVEL  = 7;
    private static final int LINGER_LEVEL  = 9;

    private static final Map<MobEffect, Integer> EFFECT_LEVEL = new HashMap<>();

    static {
        put(1, MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP);
        put(2, MobEffects.WATER_BREATHING, MobEffects.SLOW_FALLING, MobEffects.FIRE_RESISTANCE);
        put(3, MobEffects.HEAL, MobEffects.POISON);
        put(4, MobEffects.DAMAGE_BOOST, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN);
        put(5, MobEffects.REGENERATION);
        put(6, MobEffects.INVISIBILITY, MobEffects.DAMAGE_RESISTANCE); // resistance → Turtle Master
        put(8, MobEffects.HARM);
    }

    private static void put(int level, MobEffect... effects) {
        for (MobEffect e : effects) EFFECT_LEVEL.put(e, level);
    }

    /** Требуемый уровень Алхимика для варки данного результата (0 — без ограничений). */
    public static int requiredLevel(ItemStack resultStack) {
        Potion potion = PotionUtils.getPotion(resultStack);
        int level = 0;
        for (MobEffectInstance e : potion.getEffects()) {
            level = Math.max(level, EFFECT_LEVEL.getOrDefault(e.getEffect(), 0));
        }
        var key = net.minecraftforge.registries.ForgeRegistries.POTIONS.getKey(potion);
        if (key != null) {
            String path = key.getPath();
            if (path.startsWith("long_"))   level = Math.max(level, LONG_LEVEL);
            if (path.startsWith("strong_")) level = Math.max(level, STRONG_LEVEL);
        }
        if (resultStack.is(Items.SPLASH_POTION))    level = Math.max(level, SPLASH_LEVEL);
        if (resultStack.is(Items.LINGERING_POTION)) level = Math.max(level, LINGER_LEVEL);
        return level;
    }
}
