package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

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
 * Лестница доступа Алхимика (path {@link Path#MAGIC}, spec {@link Spec#ALCHEMIST}) —
 * гейтинг ВАРКИ по уровню специализации. Требуемый уровень = максимум из базового
 * уровня эффекта (ключ — {@code Holder<MobEffect>}), модификаторов long(5)/strong(6),
 * splash(7), lingering(9). Turtle Master гейтится по resistance(6). База — ур.0.
 * Данные зелья — из {@link DataComponents#POTION_CONTENTS}.
 */
public class PotionTierMap {

    private static final int LONG_LEVEL   = 5;
    private static final int STRONG_LEVEL = 6;
    private static final int SPLASH_LEVEL = 7;
    private static final int LINGER_LEVEL = 9;

    private static final Map<Holder<MobEffect>, Integer> EFFECT_LEVEL = new HashMap<>();

    static {
        put(1, MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP);
        put(2, MobEffects.WATER_BREATHING, MobEffects.SLOW_FALLING, MobEffects.FIRE_RESISTANCE);
        put(3, MobEffects.HEAL, MobEffects.POISON);
        put(4, MobEffects.DAMAGE_BOOST, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN);
        put(5, MobEffects.REGENERATION);
        put(6, MobEffects.INVISIBILITY, MobEffects.DAMAGE_RESISTANCE); // resistance → Turtle Master
        put(8, MobEffects.HARM);
    }

    @SafeVarargs
    private static void put(int level, Holder<MobEffect>... effects) {
        for (Holder<MobEffect> e : effects) EFFECT_LEVEL.put(e, level);
    }

    /** Требуемый уровень Алхимика для варки данного результата (0 — без ограничений). */
    public static int requiredLevel(ItemStack resultStack) {
        PotionContents contents = resultStack.get(DataComponents.POTION_CONTENTS);
        int level = 0;
        if (contents != null) {
            for (MobEffectInstance e : contents.getAllEffects()) {
                level = Math.max(level, EFFECT_LEVEL.getOrDefault(e.getEffect(), 0));
            }
            String path = contents.potion()
                    .flatMap(Holder::unwrapKey).map(k -> k.location().getPath()).orElse("");
            if (path.startsWith("long_"))   level = Math.max(level, LONG_LEVEL);
            if (path.startsWith("strong_")) level = Math.max(level, STRONG_LEVEL);
        }
        if (resultStack.is(Items.SPLASH_POTION))    level = Math.max(level, SPLASH_LEVEL);
        if (resultStack.is(Items.LINGERING_POTION)) level = Math.max(level, LINGER_LEVEL);
        return level;
    }
}
