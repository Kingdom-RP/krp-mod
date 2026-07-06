package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Мягкая интеграция мода Small Ships — без хард-зависимости. Крафт парусов и
 * кораблей идёт Мастеровому ({@link Spec#CRAFTSMAN}): XP ({@link ItemCraftMap}) +
 * гейт изъятия результата ({@link ItemCraftTierMap}, ловится {@code SlotMixin.mayPickup}).
 * Уровни (заданы вручную): Cog L4, Brigg L5, Galley L6, Drakkar L7; парус L3. XP растёт
 * с уровнем. ⚠️ Скорость всех кораблей почти равна (конфиг maxSpeed: Cog/Galley/Drakkar 30,
 * Brigg 35) — вики km/h-таблица устарела; различия в HP/cargo/пушках/абилках.
 * <p>
 * Пушка и ядро (cannon/cannon_ball) — крафт отключён оверрайдом рецептов
 * ({@code data/smallships/recipe/*}), в прокачке не участвуют. Активна только если
 * мод загружен.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class SmallShipsCompat {

    public static final String MODID = "smallships";

    /** Породы дерева, для которых мод даёт корабли (незер-дерева нет). */
    private static final String[] WOODS = {
            "oak", "spruce", "birch", "jungle", "acacia",
            "dark_oak", "mangrove", "cherry", "bamboo"
    };

    private SmallShipsCompat() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(SmallShipsCompat::register);
    }

    private static void register() {
        // Парус — текстильный компонент (шерсть+брёвна+поводок)
        craftsman(MODID + ":sail", 3f, 3);

        // Корабли — уровни заданы вручную, XP растёт с уровнем
        ship("cog", 8f, 4);
        ship("brigg", 10f, 5);
        ship("galley", 13f, 6);
        ship("drakkar", 16f, 7);
    }

    /** Один тип корабля по всем породам дерева. */
    private static void ship(String type, float xp, int level) {
        for (String wood : WOODS) {
            craftsman(MODID + ":" + wood + "_" + type, xp, level);
        }
    }

    private static void craftsman(String id, float xp, int level) {
        ItemCraftMap.addById(id, new CraftEntry(Path.CRAFT, Spec.CRAFTSMAN, xp));
        ItemCraftTierMap.addById(id, new SpecRequirement(Spec.CRAFTSMAN, level));
    }
}
