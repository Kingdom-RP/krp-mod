package com.kingdomrp.core.data.map;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    private static final Set<Item> BANNED = Set.of(
            // взрывчатка / грифинг
            Items.TNT, Items.TNT_MINECART, Items.END_CRYSTAL,
            // item-лаг
            Items.HOPPER, Items.HOPPER_MINECART,
            // лаг / летающие машины
            Items.OBSERVER, Items.PISTON, Items.STICKY_PISTON,
            // item/снаряд-машины
            Items.DISPENSER, Items.DROPPER
    );

    private BannedCraftMap() {}

    public static boolean isBanned(Item item) {
        return BANNED.contains(item);
    }
}
