package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.item.CharterItem;
import com.kingdomrp.core.kingdom.item.LordRingItem;
import net.minecraft.world.item.BlockItem;
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
 *
 * <p>Королевства: {@link #CHARTER} (хартия), {@link #LORD_RING} (кольцо лорда),
 * {@link #KINGDOM_BLOCK_ITEM} (блок королевства).</p>
 */
public class KRPItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KingdomRPCore.MODID);

    public static final DeferredItem<Item> IRON_RING = ITEMS.registerSimpleItem("iron_ring");

    /** Фрагмент черепа визер-скелета — падает вместо черепа (Wither Skull Overhaul-порт). */
    public static final DeferredItem<Item> WITHER_SKULL_FRAGMENT = ITEMS.registerSimpleItem("wither_skull_fragment");

    public static final DeferredItem<CharterItem> CHARTER =
            ITEMS.registerItem("charter", CharterItem::new, new Item.Properties());

    public static final DeferredItem<LordRingItem> LORD_RING =
            ITEMS.registerItem("lord_ring", LordRingItem::new, new Item.Properties());

    public static final DeferredItem<BlockItem> KINGDOM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("kingdom_block", KRPBlocks.KINGDOM_BLOCK);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(KRPItems::addToTabs);
    }

    private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(IRON_RING);
            event.accept(WITHER_SKULL_FRAGMENT);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHARTER);
            event.accept(LORD_RING);
            event.accept(KINGDOM_BLOCK_ITEM);
        }
    }
}
