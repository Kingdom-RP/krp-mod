package com.kingdomrp.core.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemCombinerMenu.class, remap = false)
public interface ItemCombinerMenuAccessor {
    @Accessor("inputSlots")
    Container getInputSlots();

    @Accessor("player")
    Player getPlayer();
}