package com.kingdomrp.core.mixin;

import com.kingdomrp.core.data.EnchantTierMap;
import com.kingdomrp.core.system.EnchantSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Фильтрация пула чар, предлагаемых столом зачарования, по уровню Зачарователя
 * (path Магия). Стол физически не предлагает недоступные по тиру чары —
 * устраняет ситуацию «вариант есть, но кликнуть нельзя» (нет софтлока).
 * <p>
 * Срабатывает ТОЛЬКО когда генерация инициирована столом: уровень кладётся в
 * {@link EnchantSystem#tableLevel()} из {@code EnchantmentMenuMixin} вокруг
 * {@code getEnchantmentList}. Для прочих источников ({@code enchantItem} лута,
 * жителей, рыбалки) ThreadLocal пуст → фильтр не трогает ваниль.
 */
@Mixin(value = EnchantmentHelper.class, remap = false)
public class EnchantmentHelperMixin {

    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"), remap = false)
    private static void krp$filterByTier(int power, ItemStack stack,
                                         java.util.stream.Stream<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> pool,
                                         CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        Integer level = EnchantSystem.tableLevel();
        if (level == null) return;

        List<EnchantmentInstance> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;

        // Книга на столе доступна только с ур.3 — ниже пул пуст (стол не предлагает чары).
        if (stack.is(Items.BOOK) && level < EnchantSystem.BOOK_TABLE_LEVEL) {
            list.clear();
            return;
        }

        list.removeIf(ei -> EnchantTierMap.requiredForEnchant(ei.enchantment, ei.level) > level);
    }
}
