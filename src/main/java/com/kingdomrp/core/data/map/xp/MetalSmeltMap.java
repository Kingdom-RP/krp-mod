package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XP Кузнеца (path {@link Path#CRAFT}) за выплавку металла в печи. Ключ —
 * результат плавки; XP начисляется за штуку × количество при изъятии из печи
 * (см. {@code FurnaceResultSlotMixin} → {@link com.kingdomrp.core.system.XPSystem#onMetalSmelted}).
 * <p>
 * Плавка НЕ гейтится уровнем (нужна всем и рано) — даём только XP.
 */
public class MetalSmeltMap {

    private static final Map<Item, Float> MAP = new HashMap<>();
    private static final Map<Item, Float> OVERRIDE = new HashMap<>();
    private static final List<Map.Entry<TagKey<Item>, Float>> OVERRIDE_TAGS = new ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item, float xp) { OVERRIDE.put(item, xp); }
    public static void overrideTag(TagKey<Item> tag, float xp) { OVERRIDE_TAGS.add(Map.entry(tag, xp)); }
    /** Явные BASE-записи (для экспорта датапака). */
    public static Map<Item, Float> baseEntries() { return MAP; }

    static {
        MAP.put(Items.IRON_INGOT, 2f);
        MAP.put(Items.GOLD_INGOT, 2f);
        MAP.put(Items.COPPER_INGOT, 1f);
        MAP.put(Items.NETHERITE_SCRAP, 4f);
    }

    /** XP за единицу выплавленного металла, либо 0, если не относится к Кузнецу. */
    public static float get(Item item) {
        Float o = OVERRIDE.get(item);
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (item.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        return MAP.getOrDefault(item, 0f);
    }
}
