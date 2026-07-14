package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Лестница доступа Повара (path Промысел) — гейтинг ПРОИЗВОДСТВА еды
 * по уровню специализации {@link Spec#COOK}. Один маппинг обслуживает
 * и крафт (верстак), и готовку (печь/коптильня/костёр) — ключ всегда
 * результирующий предмет.
 * <p>
 * Уровни выведены из ценности еды {@code голод + насыщение(факт)},
 * где {@code насыщение(факт) = голод × satModifier × 2}:
 * <ul>
 *   <li>ур.0 — печенье, сушёные водоросли, хлеб — без ограничений (не регистрируем).
 *       Хлеб намеренно оставлен на ур.0 как базовый источник еды без прокачки.</li>
 *   <li>ур.1 — печёная картошка, жареная треска, жареный кролик (~11)</li>
 *   <li>ур.2 — тыквенный пирог, супы, жареная курица (~13)</li>
 *   <li>ур.3 — жареный лосось, жареная баранина, торт (~15)</li>
 *   <li>ур.4 — стейк, жареная свинина (~21)</li>
 *   <li>ур.5 — рагу из кролика (~22)</li>
 * </ul>
 */
public class FoodTierMap {

    private static final Map<Item, FoodTierEntry> MAP = new HashMap<>();
    private static final Map<Item, FoodTierEntry> OVERRIDE = new HashMap<>();
    private static final java.util.List<Map.Entry<net.minecraft.tags.TagKey<Item>, FoodTierEntry>> OVERRIDE_TAGS = new java.util.ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item, FoodTierEntry e) { OVERRIDE.put(item, e); }
    public static void overrideTag(net.minecraft.tags.TagKey<Item> tag, FoodTierEntry e) { OVERRIDE_TAGS.add(Map.entry(tag, e)); }
    public static Map<Item, FoodTierEntry> baseEntries() { return MAP; }

    static {
        // Ур.1: печёная картошка, жареная треска, жареный кролик
        // (хлеб НЕ ограничен — базовый способ сделать еду без прокачки навыка)
        register(new FoodTierEntry(Spec.COOK, 1),
                Items.BAKED_POTATO,
                Items.COOKED_COD, Items.COOKED_RABBIT);

        // Ур.2: тыквенный пирог, супы, жареная курица
        register(new FoodTierEntry(Spec.COOK, 2),
                Items.PUMPKIN_PIE, Items.MUSHROOM_STEW,
                Items.BEETROOT_SOUP, Items.SUSPICIOUS_STEW,
                Items.COOKED_CHICKEN);

        // Ур.3: жареный лосось, жареная баранина, торт
        register(new FoodTierEntry(Spec.COOK, 3),
                Items.COOKED_SALMON, Items.COOKED_MUTTON, Items.CAKE);

        // Ур.4: стейк, жареная свинина
        register(new FoodTierEntry(Spec.COOK, 4),
                Items.COOKED_BEEF, Items.COOKED_PORKCHOP);

        // Ур.5: рагу из кролика
        register(new FoodTierEntry(Spec.COOK, 5),
                Items.RABBIT_STEW);
    }

    private static void register(FoodTierEntry entry, Item... items) {
        for (Item item : items) {
            MAP.put(item, entry);
        }
    }

    /** Требование уровня для производства предмета, либо null (ур.0 — без ограничений). */
    public static FoodTierEntry get(Item item) {
        FoodTierEntry o = OVERRIDE.get(item);
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) if (item.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        return MAP.get(item);
    }

    /** Регистрация еды по ID (мод-совместимость, напр. Farmer's Delight). No-op если предмета нет. */
    public static void addById(String id, Spec spec, int level) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, new FoodTierEntry(spec, level)));
    }
}
