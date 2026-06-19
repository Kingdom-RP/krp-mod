package com.kingdomrp.core.data;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.HashMap;
import java.util.Map;

/**
 * Маппинг XP Алхимика (path {@link Path#MAGIC}) за варку зелья. XP за ШТУКУ
 * полученного зелья (×количество бутылок), по ценности эффекта.
 * <p>
 * 1.21: данные зелья — компонент {@link DataComponents#POTION_CONTENTS}
 * ({@code PotionContents}); эффекты — {@code getAllEffects()}, ключ —
 * {@code Holder<MobEffect>}.
 * <ul>
 *   <li>База (awkward/mundane/thick) = 2</li>
 *   <li>T1 (night vision/swiftness/leaping/slow falling/water breathing) = 5</li>
 *   <li>T2 (fire res/healing/poison/weakness/invisibility/slowness) = 8</li>
 *   <li>T3 (strength/regeneration/turtle master/harming) = 12</li>
 *   <li>Модификатор (long/strong/splash/lingering) = +2</li>
 * </ul>
 */
public class BrewXPMap {

    private static final float BASE_XP = 2f;
    private static final float MODIFIER_BONUS = 2f;

    private static final Map<Holder<MobEffect>, Float> EFFECT_XP = new HashMap<>();

    static {
        put(5f, MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP,
                MobEffects.SLOW_FALLING, MobEffects.WATER_BREATHING);
        put(8f, MobEffects.FIRE_RESISTANCE, MobEffects.HEAL, MobEffects.POISON,
                MobEffects.WEAKNESS, MobEffects.INVISIBILITY, MobEffects.MOVEMENT_SLOWDOWN);
        put(12f, MobEffects.DAMAGE_BOOST, MobEffects.REGENERATION,
                MobEffects.DAMAGE_RESISTANCE, MobEffects.HARM);
    }

    @SafeVarargs
    private static void put(float xp, Holder<MobEffect>... effects) {
        for (Holder<MobEffect> e : effects) EFFECT_XP.put(e, xp);
    }

    /** XP за одну бутылку данного результата. */
    public static float get(ItemStack resultStack) {
        PotionContents contents = resultStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return BASE_XP;

        float xp = BASE_XP;
        for (MobEffectInstance e : contents.getAllEffects()) {
            xp = Math.max(xp, EFFECT_XP.getOrDefault(e.getEffect(), BASE_XP));
        }
        if (hasModifier(resultStack, contents)) xp += MODIFIER_BONUS;
        return xp;
    }

    private static boolean hasModifier(ItemStack stack, PotionContents contents) {
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return true;
        return contents.potion()
                .flatMap(Holder::unwrapKey)
                .map(key -> key.location().getPath())
                .map(path -> path.startsWith("long_") || path.startsWith("strong_"))
                .orElse(false);
    }
}
