package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.block.KingdomMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Реестр меню мода. */
public class KRPMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KingdomRPCore.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<KingdomMenu>> KINGDOM_BLOCK =
            MENUS.register("kingdom_block", () -> IMenuTypeExtension.create(KingdomMenu::new));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
