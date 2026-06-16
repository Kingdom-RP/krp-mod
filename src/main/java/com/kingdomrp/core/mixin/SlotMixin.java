package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.RestrictionSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гейт КРАФТА по уровню специализации — блокируем ИЗЪЯТИЕ результата, не пряча его.
 * <p>
 * Предмет в слоте результата виден (игрок понимает, что мог бы скрафтить), но если
 * он недоступен по уровню — забрать нельзя НИКАК: обычный клик, shift-click, выброс
 * по Q и свап цифрами в ванили ВСЕ проходят через {@link Slot#mayPickup(Player)}
 * (напрямую в {@code AbstractContainerMenu.doClick} либо через
 * {@code safeTake}/{@code tryRemove}). Возвращаем {@code false} → блок + предупреждение
 * в чат (троттлится). Раньше гейт стоял на {@code ItemCraftedEvent}→{@code burnCraft},
 * но он не ловил shift-click (см. «Частые ошибки» №13/№15).
 * <p>
 * Цель — базовый {@link Slot} (его {@code mayPickup} наследует {@link ResultSlot},
 * не переопределяя), с guard'ом {@code instanceof ResultSlot}, чтобы трогать только
 * слоты результата крафта (верстак + сетка 2×2). {@code mayPickup} зовётся лишь при
 * попытке взаимодействия, не в тик/рендер — спама нет.
 */
@Mixin(value = Slot.class, remap = false)
public class SlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$gateCraftPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ResultSlot)) return;
        ItemStack result = ((Slot) (Object) this).getItem();
        if (result.isEmpty()) return;

        if (RestrictionSystem.isCraftBlocked(player, result)) {
            if (!player.level().isClientSide()) {
                RestrictionSystem.warnCraftBlocked(player, result);
            }
            cir.setReturnValue(false);
        }
    }
}
