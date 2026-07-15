package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.CharterData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data-компоненты мода. {@link #CHARTER} — подпись/данные хартии королевства. */
public class KRPComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KingdomRPCore.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CharterData>> CHARTER =
            COMPONENTS.register("charter", () -> DataComponentType.<CharterData>builder()
                    .persistent(CharterData.CODEC)
                    .networkSynchronized(CharterData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
