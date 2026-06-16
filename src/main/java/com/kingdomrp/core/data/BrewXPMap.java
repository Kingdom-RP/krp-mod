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
 * Маппинг XP Алхимика (path {@link Path#MAGIC}) за варку зелья.
 * XP начисляется ЗА ШТУКУ полученного зелья (×количество сваренных бутылок),
 * по ценности эффекта. Ключ — {@link MobEffect} результата (long/strong варианты
 * наследуют XP базового эффекта, плюс модификатор-бонус).
 * <p>
 * Тиры:
 * <ul>
 *   <li>База (awkward/mundane/thick — без эффектов) = 2</li>
 *   <li>T1 (night vision/swiftness/leaping/slow falling/water breathing) = 5</li>
 *   <li>T2 (fire res/healing/poison/weakness/invisibility/slowness) = 8</li>
 *   <li>T3 (strength/regeneration/turtle master/harming) = 12</li>
 *   <li>Модификатор (long/strong/splash/lingering) = +2 к базе результата</li>
 * </ul>
 */
public class BrewXPMap {

    private static final float BASE_XP = 2f;
    private static final float MODIFIER_BONUS = 2f;

    private static final Map<MobEffect, Float> EFFECT_XP = new HashMap<>();

    static {
        // T1 = 5
        put(5f, MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP,
                MobEffects.SLOW_FALLING, MobEffects.WATER_BREATHING);
        // T2 = 8
        put(8f, MobEffects.FIRE_RESISTANCE, MobEffects.HEAL, MobEffects.POISON,
                MobEffects.WEAKNESS, MobEffects.INVISIBILITY, MobEffects.MOVEMENT_SLOWDOWN);
        // T3 = 12
        put(12f, MobEffects.DAMAGE_BOOST, MobEffects.REGENERATION,
                MobEffects.DAMAGE_RESISTANCE, MobEffects.HARM);
    }

    private static void put(float xp, MobEffect... effects) {
        for (MobEffect e : effects) EFFECT_XP.put(e, xp);
    }

    /** XP за одну бутылку данного результата. */
    public static float get(ItemStack resultStack) {
        Potion potion = PotionUtils.getPotion(resultStack);
        float xp = BASE_XP;
        for (MobEffectInstance e : potion.getEffects()) {
            xp = Math.max(xp, EFFECT_XP.getOrDefault(e.getEffect(), BASE_XP));
        }
        if (hasModifier(resultStack, potion)) xp += MODIFIER_BONUS;
        return xp;
    }

    private static boolean hasModifier(ItemStack stack, Potion potion) {
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return true;
        var key = net.minecraftforge.registries.ForgeRegistries.POTIONS.getKey(potion);
        if (key == null) return false;
        String path = key.getPath();
        return path.startsWith("long_") || path.startsWith("strong_");
    }
}
