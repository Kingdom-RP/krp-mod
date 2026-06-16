package com.kingdomrp.core.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Входной слот сырья печи/коптильни/плавильни с гейтом Повара И Кузнеца.
 * <p>
 * Подменяет ванильный {@link Slot} для слота-ингредиента в
 * {@code AbstractFurnaceMenu} (через redirect конструктора, см.
 * {@code AbstractFurnaceMenuMixin}). Запрещает класть сырьё, чей результат
 * выплавки недоступен по уровню: ЕДА — по уровню Повара ({@code FoodTierMap}),
 * МЕТАЛЛ — по уровню Кузнеца ({@code SmeltTierMap}). Сырьё просто не попадает в
 * печь, поэтому нет ни софтлока (готовый продукт некому забрать), ни обхода через
 * разрушение печи.
 * <p>
 * Гейт на {@code mayPlace} автоматически покрывает и обычную укладку, и
 * shift-click: ванильный {@code moveItemStackTo} уважает {@code mayPlace}.
 */
public class CookGatedInputSlot extends Slot {

    private final Player player;

    public CookGatedInputSlot(Player player, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        Item result = CookSystem.smeltResult(player.level(), stack);
        if (result != null) {
            // Повар — еда
            if (!CookSystem.canProduce(player, result)) {
                if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
                    CookSystem.sendRestriction(sp, result);
                }
                return false;
            }
            // Кузнец — металл
            if (RestrictionSystem.isSmeltBlocked(player, result)) {
                if (!player.level().isClientSide()) {
                    RestrictionSystem.sendSmeltRestriction(player, result);
                }
                return false;
            }
        }
        return super.mayPlace(stack);
    }
}
