package com.kingdomrp.core.kingdom.upkeep;

/** Характеристики содержания королевства. Диапазон 0..MAX, старт DEFAULT. */
public enum Characteristic {
    FOOD,        // продовольствие
    MATERIALS,   // строительные материалы
    PROSPERITY;  // довольствие

    public static final float MAX = 1000f;
    public static final float DEFAULT = 500f;

    public final int index = ordinal();

    public static final Characteristic[] VALUES = values();

    /** Ценность одного предмета stack для этой характеристики (0 = не подходит). */
    public float value(net.minecraft.world.item.ItemStack stack) {
        return switch (this) {
            case FOOD -> UpkeepValues.foodValue(stack);
            case MATERIALS -> UpkeepValues.materialValue(stack);
            case PROSPERITY -> UpkeepValues.prosperityValue(stack);
        };
    }

    public boolean accepts(net.minecraft.world.item.ItemStack stack) {
        return value(stack) > 0f;
    }
}
