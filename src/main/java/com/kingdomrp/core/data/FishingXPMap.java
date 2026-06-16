package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * Маппинг опыта рыбалки для пути Промысел (специализация Рыбак).
 * <p>
 * XP берётся за самый ценный предмет улова:
 * хлам < обычная рыба < редкая рыба < сокровища.
 * Сокровища отдельно помечены через {@link #isTreasure(Item)} —
 * эффект «двойной улов» их не дублирует.
 */
public class FishingXPMap {

    public static final float JUNK_XP    = 3f;  // палка, кожа, нить, кость, гнилая плоть и т.п.
    public static final float COMMON_XP  = 5f;  // обычная рыба (cod/salmon)
    public static final float RARE_XP    = 8f;  // редкая рыба (tropical/pufferfish)
    public static final float TREASURE_XP = 25f; // сокровища

    private static final Set<Item> COMMON_FISH = new HashSet<>();
    private static final Set<Item> RARE_FISH = new HashSet<>();
    private static final Set<Item> TREASURE = new HashSet<>();

    static {
        // Обычная рыба
        register(COMMON_FISH, Items.COD, Items.SALMON);

        // Редкая рыба
        register(RARE_FISH, Items.TROPICAL_FISH, Items.PUFFERFISH);

        // Сокровища — редкий и ценный улов
        register(TREASURE, Items.ENCHANTED_BOOK, Items.NAME_TAG,
                Items.SADDLE, Items.NAUTILUS_SHELL,
                Items.BOW, Items.FISHING_ROD);
    }

    private static void register(Set<Item> set, Item... items) {
        for (Item item : items) set.add(item);
    }

    /** XP за конкретный предмет улова. */
    public static float get(Item item) {
        if (TREASURE.contains(item)) return TREASURE_XP;
        if (RARE_FISH.contains(item)) return RARE_XP;
        if (COMMON_FISH.contains(item)) return COMMON_XP;
        return JUNK_XP;
    }

    /** Сокровище ли это (не дублируется эффектом «двойной улов»). */
    public static boolean isTreasure(Item item) {
        return TREASURE.contains(item);
    }
}
