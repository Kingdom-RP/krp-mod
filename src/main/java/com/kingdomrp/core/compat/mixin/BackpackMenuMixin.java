package com.kingdomrp.core.compat.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Фикс мода Backpacks: открытый рюкзак нельзя перемещать в собственный слот. Цель
 * по СТРОКЕ, применяется только если backpacks загружен (см.
 * {@link com.kingdomrp.core.compat.BackpacksMixinPlugin}).
 * <p>
 * Баг: рюкзак в хотбар-слоте открыт; хоткей «1» = {@link ClickType#SWAP} кладёт
 * предмет из рюкзака в слот открытого рюкзака → рюкзак уезжает внутрь себя, на
 * закрытии контент пишется рекурсивно и предмет теряется. Отменяем любой {@code
 * clicked}, который перемещает сам открытый рюкзак ({@code self}).
 */
@Mixin(targets = "live.hisui.backpacks.menu.BackpackMenu", remap = false)
public abstract class BackpackMenuMixin extends net.minecraft.world.inventory.AbstractContainerMenu {

    @Shadow @Final private ItemStack self;

    protected BackpackMenuMixin() { super(null, 0); }   // не вызывается (mixin)

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void krp$protectOpenBackpack(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (self == null || self.isEmpty()) return;

        // Хоткей-своп (SWAP): button = целевой слот хотбара/оффхенда. Если там сам
        // открытый рюкзак — запрещаем, иначе он уедет в собственный контейнер.
        if (type == ClickType.SWAP && player.getInventory().getItem(button) == self) {
            ci.cancel();
            return;
        }

        // Любое прямое взаимодействие со слотом, где лежит открытый рюкзак
        // (drag-out / shift-move), — тоже запрещаем.
        if (slotId >= 0 && slotId < this.slots.size() && this.getSlot(slotId).getItem() == self) {
            ci.cancel();
        }
    }
}
