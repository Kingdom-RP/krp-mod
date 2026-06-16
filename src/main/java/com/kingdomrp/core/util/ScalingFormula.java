package com.kingdomrp.core.util;

public class ScalingFormula {

    /**
     * Убывающая отдача.
     * @param level    уровень специализации
     * @param maxValue максимальное значение (предел к которому стремится)
     * @param k        скорость роста (выше = быстрее набирает, рекомендую 0.5-1.0)
     * @return значение от 0 до maxValue
     */
    public static float compute(int level, float maxValue, float k) {
        if (level <= 0) return 0f;
        return maxValue * (1f - 1f / (1f + k * level));
    }
}