package com.kingdomrp.core.mixin;

import com.kingdomrp.core.data.EnchantXPMap;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.data.RepairXPMap;
import com.kingdomrp.core.system.EnchantSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * XP на точильном камне (слот результата {@code GrindstoneMenu$4.onTake}):
 * Зачарователь — половинный XP за снятые НЕ-проклятые чары
 * ({@value EnchantSystem#GRINDSTONE_XP_FRACTION}); Кузнец — XP за ремонт
 * (объединение двух предметов) по материалу ({@link RepairXPMap}).
 * ⚠️ Номер анонимного класса ($4) перепроверять по байткоду при обновлениях.
 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4", remap = false)
public class GrindstoneMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"), remap = false)
    private void krp$onGrindstoneTake(Player player, ItemStack result, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.containerMenu instanceof GrindstoneMenu menu)) return;

        // Слоты 0 и 1 — входные (repairSlots), добавлены первыми.
        ItemStack a = menu.getSlot(0).getItem();
        ItemStack b = menu.getSlot(1).getItem();

        // Зачарователь — XP за снятые НЕ-проклятые чары.
        float enchXp = (EnchantXPMap.xpNonCurse(a) + EnchantXPMap.xpNonCurse(b))
                * EnchantSystem.GRINDSTONE_XP_FRACTION;
        if (enchXp > 0f) XPSystem.giveXP(serverPlayer, Path.MAGIC, enchXp);

        // Кузнец — XP за ремонт (объединение двух предметов в один).
        if (!a.isEmpty() && !b.isEmpty()) {
            XPSystem.giveXP(serverPlayer, Path.CRAFT, RepairXPMap.get(result));
        }
    }
}
