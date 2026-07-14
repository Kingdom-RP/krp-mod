package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.XPSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * XP Ремесла за резку на камнерезке. Result-слот камнерезки — анонимный
 * {@code StonecutterMenu$2} (переопределяет {@code onTake}, не шлёт
 * {@code ItemCraftedEvent}). Инжектим в {@code onTake} — XP через {@link XPSystem#onStonecut}.
 * Гейт уровня — {@link SlotMixin} (mayPickup по контейнеру ResultContainer).
 */
@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2", remap = false)
public class StonecutterResultSlotMixin {

    // HEAD матчится гарантированно (offset 0) — надёжнее TAIL/RETURN. require=0/expect=0:
    // цель — анонимный inner-класс; при любом промахе НЕ крашим меню (камнерез фликал и
    // не ставился), просто без XP. stack = изымаемый результат.
    @Inject(method = "onTake", at = @At("HEAD"), remap = false, require = 0, expect = 0)
    private void krp$stonecutXp(Player player, ItemStack stack, CallbackInfo ci) {
        XPSystem.onStonecut(player);
    }
}
