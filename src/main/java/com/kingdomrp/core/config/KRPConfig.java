package com.kingdomrp.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class KRPConfig {

    public static final ForgeConfigSpec SPEC;

    // XP и прокачка
    public static final ForgeConfigSpec.DoubleValue BASE_XP;
    public static final ForgeConfigSpec.DoubleValue XP_CURVE;

    // Штраф при смерти
    public static final ForgeConfigSpec.IntValue DEATH_PENALTY_DURATION;
    public static final ForgeConfigSpec.DoubleValue DEATH_XP_MULTIPLIER;

    // Магия
    public static final ForgeConfigSpec.DoubleValue BREW_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ENCHANT_TABLE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ENCHANT_ANVIL_BASE_CHANCE;

    // Общее
    public static final ForgeConfigSpec.BooleanValue RESTRICTIONS_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Настройки прокачки").push("progression");
        BASE_XP = builder
                .comment("Базовое количество XP до первого уровня")
                .defineInRange("baseXP", 100.0, 10.0, 10000.0);
        XP_CURVE = builder
                .comment("Коэффициент роста кривой XP (required = baseXP * curve^level)")
                .defineInRange("xpCurve", 1.5, 1.0, 3.0);
        DEATH_PENALTY_DURATION = builder
                .comment("Длительность дебаффа к опыту после смерти, в тиках (6000 = 5 минут; 0 = выкл.)")
                .defineInRange("deathPenaltyDuration", 6000, 0, 1728000);
        DEATH_XP_MULTIPLIER = builder
                .comment("Множитель получаемого опыта при дебаффе смерти (0.5 = -50%)")
                .defineInRange("deathXpMultiplier", 0.5, 0.0, 1.0);
        builder.pop();

        builder.comment("Настройки магии").push("magic");
        BREW_BASE_CHANCE = builder
                .comment("Базовый шанс успешной варки зелья без навыка")
                .defineInRange("brewBaseChance", 0.4, 0.0, 1.0);
        ENCHANT_TABLE_BASE_CHANCE = builder
                .comment("Базовый шанс успешного зачарования на столе без навыка")
                .defineInRange("enchantTableBaseChance", 0.35, 0.0, 1.0);
        ENCHANT_ANVIL_BASE_CHANCE = builder
                .comment("Базовый шанс успешного зачарования книгой на наковальне без навыка")
                .defineInRange("enchantAnvilBaseChance", 0.2, 0.0, 1.0);
        builder.pop();

        builder.comment("Общие настройки").push("general");
        RESTRICTIONS_ENABLED = builder
                .comment("Включить ограничения по тирам (false = все предметы доступны всем)")
                .define("restrictionsEnabled", true);
        builder.pop();

        SPEC = builder.build();
    }
}