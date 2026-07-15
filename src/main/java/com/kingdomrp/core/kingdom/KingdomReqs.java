package com.kingdomrp.core.kingdom;

import com.kingdomrp.core.capability.PlayerData;

import java.util.Map;

/**
 * Требования к будущему королю при подписании хартии:
 * <ul>
 *   <li>≥1 навык (spec) максимального уровня 10;</li>
 *   <li>≥2 ДРУГИХ навыка уровня ≥5 (навык из п.1 не считается);</li>
 *   <li>суммарный уровень (сумма уровней всех навыков) ≥25.</li>
 * </ul>
 */
public final class KingdomReqs {

    public static final int MAXED_LEVEL = 10;
    public static final int SUPPORT_LEVEL = 5;
    public static final int SUPPORT_COUNT = 2;
    public static final int TOTAL_LEVEL = 25;

    private KingdomReqs() {}

    /** Результат проверки: ok + i18n-ключ причины отказа (null при ok). */
    public record Result(boolean ok, String failKey) {
        public static final Result OK = new Result(true, null);
        public static Result fail(String key) { return new Result(false, key); }
    }

    public static Result check(PlayerData data) {
        Map<String, Integer> specs = data.getSpecializationLevels();

        boolean hasMaxed = false;
        int support = 0;        // навыки ≥5, НЕ засчитывая один максимальный
        int total = 0;

        for (int lvl : specs.values()) {
            total += lvl;
            if (!hasMaxed && lvl >= MAXED_LEVEL) {
                hasMaxed = true;    // максимальный не идёт в счёт support
                continue;
            }
            if (lvl >= SUPPORT_LEVEL) support++;
        }

        if (!hasMaxed)              return Result.fail("kingdomrp.charter.req.maxed");
        if (support < SUPPORT_COUNT) return Result.fail("kingdomrp.charter.req.support");
        if (total < TOTAL_LEVEL)     return Result.fail("kingdomrp.charter.req.total");
        return Result.OK;
    }
}
