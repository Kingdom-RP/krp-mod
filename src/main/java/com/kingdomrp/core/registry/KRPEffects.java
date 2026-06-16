package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Реестр кастомных эффектов мода.
 *
 * <p>{@link #DEATH_XP_PENALTY} — дебафф «Смертельная усталость»: маркер, по
 * наличию которого {@code XPSystem.giveXP} снижает получаемый опыт. Сам по себе
 * атрибуты не меняет. Накладывается при возрождении после смерти; список
 * curative-items при наложении очищается, поэтому ведро молока его НЕ снимает
 * (молоко в Forge зовёт {@code curePotionEffects(stack)}, а не
 * {@code removeAllEffects()}), при этом все ванильные эффекты молоко снимает как
 * обычно.</p>
 */
public class KRPEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, KingdomRPCore.MODID);

    public static final RegistryObject<MobEffect> DEATH_XP_PENALTY = EFFECTS.register(
            "death_xp_penalty",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A4A8A) {}
    );

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
