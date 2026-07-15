package com.kingdomrp.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class KRPConfig {

    public static final ModConfigSpec SPEC;

    // XP и прокачка
    public static final ModConfigSpec.DoubleValue BASE_XP;
    public static final ModConfigSpec.DoubleValue XP_CURVE;

    // Штраф при смерти
    public static final ModConfigSpec.IntValue DEATH_PENALTY_DURATION;
    public static final ModConfigSpec.DoubleValue DEATH_XP_MULTIPLIER;

    // Магия
    public static final ModConfigSpec.DoubleValue BREW_BASE_CHANCE;
    public static final ModConfigSpec.DoubleValue ENCHANT_TABLE_BASE_CHANCE;
    public static final ModConfigSpec.DoubleValue ENCHANT_ANVIL_BASE_CHANCE;

    // Общее
    public static final ModConfigSpec.BooleanValue RESTRICTIONS_ENABLED;

    // Проверка модов клиента (анти-чит whitelist)
    public static final ModConfigSpec.BooleanValue MOD_CHECK_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOD_WHITELIST_EXTRA;

    // Анти-грифинг
    public static final ModConfigSpec.BooleanValue ANTIGRIEF_CRAFT_BAN;
    public static final ModConfigSpec.BooleanValue ANTIGRIEF_CLOSE_END;

    // Баланс голода (важность Повара и пути Добыча)
    public static final ModConfigSpec.IntValue RESPAWN_FOOD_LEVEL;
    public static final ModConfigSpec.DoubleValue EXHAUSTION_MULTIPLIER;

    // Содержание королевства (upkeep) — потребление/день
    public static final ModConfigSpec.DoubleValue UPKEEP_FOOD_PER_RESIDENT;
    public static final ModConfigSpec.DoubleValue UPKEEP_MATERIALS_PER_CHUNK;
    public static final ModConfigSpec.DoubleValue UPKEEP_PROSPERITY_PER_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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

        builder.comment("Проверка модов подключающегося клиента (анти-чит).",
                        "Разрешённый набор = моды СЕРВЕРА + список ниже. Клиент с любым",
                        "модом вне этого набора получает кик во время хендшейка.")
                .push("modCheck");
        MOD_CHECK_ENABLED = builder
                .comment("Кикать игроков с модами вне белого списка (false = проверка выключена)")
                .define("enabled", true);
        MOD_WHITELIST_EXTRA = builder
                .comment("Дополнительно разрешённые modId на клиенте, помимо модов сервера",
                        "(клиентские моды: миникарта, шейдеры и т.п.). Пример: [\"journeymap\", \"iris\"]")
                .defineListAllowEmpty("extraAllowedMods", List.of(),
                        o -> o instanceof String);
        builder.pop();

        builder.comment("Анти-грифинг (на время релиза).").push("antiGrief");
        ANTIGRIEF_CRAFT_BAN = builder
                .comment("Запретить крафт грифинг/лаг-предметов (TNT, вагонетка с TNT, кристалл Энда,",
                        "воронки и вагонетка с воронкой, observer, поршни, раздатчик, выбрасыватель)")
                .define("craftBanEnabled", true);
        ANTIGRIEF_CLOSE_END = builder
                .comment("Закрыть доступ в измерение Энд (отмена телепорта в Энд)")
                .define("closeEnd", true);
        builder.pop();

        builder.comment("Баланс голода — повышает важность Повара и пути Добыча.").push("balance");
        RESPAWN_FOOD_LEVEL = builder
                .comment("Уровень голода после возрождения (0–20; 10 = 50%)")
                .defineInRange("respawnFoodLevel", 10, 0, 20);
        EXHAUSTION_MULTIPLIER = builder
                .comment("Множитель расхода голода за все действия (1.5 = в полтора раза быстрее)")
                .defineInRange("exhaustionMultiplier", 1.5, 1.0, 10.0);
        builder.pop();

        builder.comment("Содержание — расход за период потребления (~20 мин). Дефолт (3 жителя/",
                        "25 чанков/Σур.50) держит 500 ~3 РЕАЛЬНЫХ дня (216 периодов).").push("upkeep");
        UPKEEP_FOOD_PER_RESIDENT = builder
                .comment("Расход продовольствия за жителя за период")
                .defineInRange("foodPerResident", 0.77, 0.0, 1000.0);
        UPKEEP_MATERIALS_PER_CHUNK = builder
                .comment("Расход материалов за чанк за период")
                .defineInRange("materialsPerChunk", 0.093, 0.0, 1000.0);
        UPKEEP_PROSPERITY_PER_LEVEL = builder
                .comment("Расход довольствия за суммарный уровень жителей за период")
                .defineInRange("prosperityPerLevel", 0.046, 0.0, 1000.0);
        builder.pop();

        SPEC = builder.build();
    }
}