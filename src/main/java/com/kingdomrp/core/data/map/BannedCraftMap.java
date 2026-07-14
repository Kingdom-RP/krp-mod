package com.kingdomrp.core.data.map;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * Жёсткий бан крафта (анти-грифинг). В отличие от {@link ItemCraftTierMap}
 * (гейт по уровню специализации), эти предметы нельзя скрафтить НИКОМУ, пока
 * включён {@code antiGrief.craftBanEnabled}.
 * <p>
 * Состав — основные средства грифинга и лаг-машин ванили:
 * <ul>
 *   <li>взрывчатка: TNT, вагонетка с TNT (ломает обсидиан), кристалл Энда;</li>
 *   <li>item-лаг: воронки и вагонетка с воронкой;</li>
 *   <li>лаг/летающие машины: observer, поршень, липкий поршень;</li>
 *   <li>item/снаряд-машины: раздатчик, выбрасыватель.</li>
 * </ul>
 * Проверка по {@link Item} результата крафта в {@code RestrictionSystem.isCraftBlocked}.
 */
public final class BannedCraftMap {

    private static final Set<Item> BANNED = new HashSet<>(Set.of(
            // взрывчатка / грифинг
            Items.TNT, Items.TNT_MINECART, Items.END_CRYSTAL,
            // item-лаг
            Items.HOPPER, Items.HOPPER_MINECART,
            // лаг / летающие машины
            Items.OBSERVER, Items.PISTON, Items.STICKY_PISTON,
            // item/снаряд-машины
            Items.DISPENSER, Items.DROPPER
    ));

    private static final Set<Item> OVERRIDE = new HashSet<>();
    private static final java.util.List<net.minecraft.tags.TagKey<Item>> OVERRIDE_TAGS = new java.util.ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item) { OVERRIDE.add(item); }
    public static void overrideTag(net.minecraft.tags.TagKey<Item> tag) { OVERRIDE_TAGS.add(tag); }
    public static Set<Item> baseEntries() { return BANNED; }

    private BannedCraftMap() {}

    public static boolean isBanned(Item item) {
        if (BANNED.contains(item) || OVERRIDE.contains(item)) return true;
        for (var t : OVERRIDE_TAGS) if (item.builtInRegistryHolder().is(t)) return true;
        return false;
    }

    /** Бан крафта по ID (мод-совместимость, напр. off-theme предметы). No-op если предмета нет. */
    public static void addById(String id) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(BANNED::add);
    }
}
