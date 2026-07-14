package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
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

    // Шкала XP по редкости (общая с рыбой Tide — см. TideCompat.xpForRarity):
    public static final float JUNK_XP      = 3f;  // хлам: палка, кожа, нить, кость, гнилая плоть
    public static final float COMMON_XP    = 5f;  // обычная рыба (cod/salmon) + common Tide
    public static final float UNCOMMON_XP  = 8f;  // tropical/pufferfish + uncommon Tide
    public static final float RARE_XP      = 10f; // rare Tide
    public static final float VERY_RARE_XP = 15f; // very_rare Tide
    public static final float TREASURE_XP  = 25f; // сокровища + legendary Tide

    private static final Set<Item> COMMON_FISH = new HashSet<>();
    private static final Set<Item> UNCOMMON_FISH = new HashSet<>();
    private static final Set<Item> TREASURE = new HashSet<>();
    /** Датапак-оверрайд по конкретному предмету (перекрывает правила по редкости). */
    private static final java.util.Map<Item, Float> OVERRIDE = new java.util.HashMap<>();
    private static final java.util.List<java.util.Map.Entry<net.minecraft.tags.TagKey<Item>, Float>> OVERRIDE_TAGS = new java.util.ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item, float xp) { OVERRIDE.put(item, xp); }
    public static void overrideTag(net.minecraft.tags.TagKey<Item> tag, float xp) { OVERRIDE_TAGS.add(java.util.Map.entry(tag, xp)); }

    static {
        // Обычная рыба
        register(COMMON_FISH, Items.COD, Items.SALMON);

        // Необычная рыба
        register(UNCOMMON_FISH, Items.TROPICAL_FISH, Items.PUFFERFISH);

        // Сокровища — редкий и ценный улов
        register(TREASURE, Items.ENCHANTED_BOOK, Items.NAME_TAG,
                Items.SADDLE, Items.NAUTILUS_SHELL,
                Items.BOW, Items.FISHING_ROD);
    }

    private static void register(Set<Item> set, Item... items) {
        Collections.addAll(set, items);
    }

    /** XP за конкретный предмет улова. */
    public static float get(Item item) {
        Float o = OVERRIDE.get(item);
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (item.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        if (TREASURE.contains(item)) return TREASURE_XP;
        if (UNCOMMON_FISH.contains(item)) return UNCOMMON_XP;
        if (COMMON_FISH.contains(item)) return COMMON_XP;
        float tide = com.kingdomrp.core.compat.TideCompat.fishXP(item); // рыба Tide по редкости
        if (tide > 0f) return tide;
        return JUNK_XP;
    }

    /** Сокровище ли это (не дублируется эффектом «двойной улов»). */
    public static boolean isTreasure(Item item) {
        return TREASURE.contains(item);
    }
}
