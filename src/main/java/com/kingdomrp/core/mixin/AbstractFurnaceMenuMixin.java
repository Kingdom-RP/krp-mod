package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.CookGatedInputSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Повар: гейтинг готовки на входе печи/коптильни. Подменяем ванильный слот
 * сырья (индекс 0) на {@link CookGatedInputSlot}, который запрещает класть
 * сырьё с недоступным по уровню результатом выплавки.
 * <p>
 * Гейт на входе (а не на выходе) убирает софтлок печи и обход через её
 * разрушение, а ванильный {@code moveItemStackTo} уважает {@code mayPlace} —
 * поэтому одна проверка покрывает и обычную укладку, и shift-click.
 * <p>
 * Игрока добываем без MixinExtras/{@code @Inject} в конструктор (Mixin запрещает
 * инжект в конструктор, а MixinExtras `@Local` нет на classpath): в конструкторе
 * есть строка {@code this.level = inventory.player.level()} ДО создания слотов —
 * перехватываем чтение поля {@code Inventory.player} и кэшируем игрока в
 * {@code @Unique}-поле. Затем redirect ловит все {@code new Slot(...)}: слоты
 * инвентаря (контейнер {@link Inventory}) оставляем ванильными, единственный
 * не-{@code Inventory} — слот сырья печи — подменяем. Слоты топлива/результата —
 * отдельные классы, под redirect не попадают.
 */
@Mixin(value = AbstractFurnaceMenu.class, remap = false)
public abstract class AbstractFurnaceMenuMixin {

    @Unique
    private Player krp$player;

    @Redirect(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Inventory;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player krp$capturePlayer(Inventory inventory) {
        this.krp$player = inventory.player;
        return inventory.player;
    }

    @Redirect(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/Container;III)Lnet/minecraft/world/inventory/Slot;"),
            remap = false
    )
    private Slot krp$gateInputSlot(Container container, int slot, int x, int y) {
        if (container instanceof Inventory) {
            return new Slot(container, slot, x, y); // слоты инвентаря/хотбара
        }
        // Единственный не-инвентарный new Slot — слот сырья печи (индекс 0)
        return new CookGatedInputSlot(this.krp$player, container, slot, x, y);
    }
}
