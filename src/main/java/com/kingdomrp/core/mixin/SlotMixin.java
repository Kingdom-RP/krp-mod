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
 * Гейт КРАФТА по уровню специализации — блокировка ИЗЪЯТИЯ результата (результат
 * ВИДЕН в слоте, но забрать его нельзя, пока не хватает уровня).
 * <p>
 * Цель — базовый {@link Slot} (его {@code mayPickup} наследует {@link ResultSlot},
 * не переопределяя), guard {@code instanceof ResultSlot} ограничивает гейт слотами
 * результата крафта (верстак + сетка 2×2). Возвращаем {@code false} при
 * {@link RestrictionSystem#isCraftBlocked} → результат нельзя взять НИКАК: все
 * способы изъятия в ванили проходят через {@code Slot.mayPickup}:
 * <ul>
 *   <li>обычный клик / выброс по Q / double-click — через {@code tryRemove};</li>
 *   <li>shift-click — {@code AbstractContainerMenu.doClick} в ветке QUICK_MOVE
 *       проверяет {@code slot.mayPickup(player)} и выходит ДО {@code quickMoveStack},
 *       если он {@code false} (проверено по исходнику 1.20.1);</li>
 *   <li>свап цифрами — там же.</li>
 * </ul>
 * Сообщение в чат — на попытку (троттлится по времени), только на сервере.
 * {@code mayPickup} зовётся лишь при взаимодействии (не в тик/рендер) — спама нет.
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
