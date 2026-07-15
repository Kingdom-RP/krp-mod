package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.block.KingdomBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Реестр block entity мода. */
public class KRPBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    KingdomRPCore.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KingdomBlockEntity>> KINGDOM_BLOCK =
            BLOCK_ENTITIES.register("kingdom_block", () -> BlockEntityType.Builder.of(
                    KingdomBlockEntity::new, KRPBlocks.KINGDOM_BLOCK.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
