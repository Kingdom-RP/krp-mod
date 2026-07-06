package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Опыт за варку пива в Кеге (path Промысел, Повар). Ключ — результирующий сорт
 * пива, XP начисляется однократно на завершении варки. Гейтинг доступа к сорту —
 * через {@link FoodTierMap} (уровень Повара). Регистрируется по ID из
 * {@link com.kingdomrp.core.compat.AlcoCraftCompat}.
 */
public class BeerBrewXPMap {

    private static final Map<Item, Float> MAP = new HashMap<>();

    public static float get(Item item) {
        return MAP.getOrDefault(item, 0f);
    }

    /** Регистрация XP за варку по ID. No-op если предмета нет. */
    public static void addById(String id, float xp) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, xp));
    }
}
