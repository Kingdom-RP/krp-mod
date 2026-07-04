package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр предметов мода.
 *
 * <p>{@link #IRON_RING} — железное кольцо: промежуточный компонент крафта
 * кольчужной брони (крафтится из железных самородков, из колец — кольчуга).
 * Гейт/XP — Кузнец (см. {@code ItemCraftMap}/{@code ItemCraftTierMap}).</p>
 */
public class KRPItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KingdomRPCore.MODID);

    public static final DeferredItem<Item> IRON_RING = ITEMS.registerSimpleItem("iron_ring");

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(KRPItems::addToTabs);
    }

    private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(IRON_RING);
        }
    }
}
