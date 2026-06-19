package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр кастомных эффектов мода.
 *
 * <p>{@link #DEATH_XP_PENALTY} — дебафф «Штраф к опыту»: маркер, по наличию
 * которого {@code XPSystem.giveXP} снижает получаемый опыт. Сам по себе атрибуты
 * не меняет. Накладывается при возрождении после смерти.</p>
 */
public class KRPEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KingdomRPCore.MODID);

    public static final Holder<MobEffect> DEATH_XP_PENALTY = EFFECTS.register(
            "death_xp_penalty",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A4A8A) {}
    );

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
