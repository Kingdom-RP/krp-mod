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
 * Маппинг опыта за ГОТОВКУ (термообработку) еды для пути Промысел
 * (специализация Повар). Ключ — результат готовки в печи/коптильне/костре/котелке.
 * XP за штуку × количество при изъятии.
 * <p>
 * Шкала единая с Farmer's Delight и кривой уровней — геометрическая
 * {@link #CURVE}× за тир (см. {@link #xpForTier}). Тир — по {@link FoodTierMap}.
 */
public class FoodCookMap {

    /** Рост XP за тир — как кривая уровней (baseXP × curve^level). */
    private static final double CURVE = 1.5;

    private static final Map<Item, Float> MAP = new HashMap<>();
    /** Датапак-оверрайд (перекрывает BASE/compat, перезагружается на /reload). */
    private static final Map<Item, Float> OVERRIDE = new HashMap<>();
    private static final List<Map.Entry<TagKey<Item>, Float>> OVERRIDE_TAGS = new ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item, float xp) { OVERRIDE.put(item, xp); }
    public static void overrideTag(TagKey<Item> tag, float xp) { OVERRIDE_TAGS.add(Map.entry(tag, xp)); }

    static {
        register(xpForTier(0), Items.DRIED_KELP);
        register(xpForTier(1), Items.BAKED_POTATO, Items.COOKED_COD, Items.COOKED_RABBIT);
        register(xpForTier(2), Items.COOKED_CHICKEN);
        register(xpForTier(3), Items.COOKED_SALMON, Items.COOKED_MUTTON);
        register(xpForTier(4), Items.COOKED_BEEF, Items.COOKED_PORKCHOP);
    }

    /** XP за готовку для тира: тир0 = 1, далее 2 × {@link #CURVE}^(тир−1) (дробное). */
    public static float xpForTier(int tier) {
        if (tier <= 0) return 1f;
        return (float) (2 * Math.pow(CURVE, tier - 1));
    }

    private static void register(float xp, Item... items) {
        for (Item item : items) {
            MAP.put(item, xp);
        }
    }

    /** XP за готовку одной штуки продукта, либо 0, если предмет не относится к Повару. */
    public static float get(Item item) {
        Float o = OVERRIDE.get(item);
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (item.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        return MAP.getOrDefault(item, 0f);
    }

    /** Регистрация XP за готовку по ID (мод-совместимость). No-op если предмета нет. */
    public static void addById(String id, float xp) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, xp));
    }
}
