package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.block.KingdomBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Реестр блоков мода. {@link #KINGDOM_BLOCK} — блок королевства. */
public class KRPBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KingdomRPCore.MODID);

    // Взрыво-/поршне-устойчив: force-load-тикет не должен исчезать от постороннего разрушения.
    public static final DeferredBlock<KingdomBlock> KINGDOM_BLOCK =
            BLOCKS.register("kingdom_block", () -> new KingdomBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(50.0F, 1200.0F)
                            .pushReaction(PushReaction.BLOCK)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
