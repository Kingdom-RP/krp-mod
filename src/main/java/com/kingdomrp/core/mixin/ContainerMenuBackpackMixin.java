package com.kingdomrp.core.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Фикс мода Backpacks: открытый рюкзак терялся, если предмет из него закинуть
 * хоткеем-цифрой ({@link ClickType#SWAP}) в слот, где лежит сам открытый рюкзак —
 * он уезжал в собственный контейнер и на закрытии контент писался рекурсивно.
 * <p>
 * {@code BackpackMenu} не переопределяет {@code clicked} (наследует от
 * {@link AbstractContainerMenu}), поэтому гейтим здесь. Активно только когда меню —
 * именно {@code live.hisui.backpacks.menu.BackpackMenu} (по имени класса, без
 * хард-зависимости): запрещаем SWAP предмета в хотбар-слот, где лежит рюкзак HA.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuBackpackMixin {

    private static final String BACKPACK_MENU = "live.hisui.backpacks.menu.BackpackMenu";

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void krp$protectOpenBackpack(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type != ClickType.SWAP) return;
        if (!BACKPACK_MENU.equals(this.getClass().getName())) return;

        ItemStack dest = player.getInventory().getItem(button);
        if (dest.isEmpty()) return;
        var key = BuiltInRegistries.ITEM.getKey(dest.getItem());
        if ("backpacks".equals(key.getNamespace())) ci.cancel();   // рюкзак в целевом слоте — не свопаем
    }
}
