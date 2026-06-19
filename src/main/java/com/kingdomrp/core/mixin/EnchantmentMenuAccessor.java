package com.kingdomrp.core.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Доступ к приватному {@code getEnchantmentList} стола зачарования — нужен,
 * чтобы определить тир будущих чар ДО их наложения (для гейта по уровню
 * Зачарователя). Вызов детерминирован (тот же seed, что в ванили), поэтому
 * набор чар совпадает с тем, что наложится.
 */
@Mixin(value = EnchantmentMenu.class, remap = false)
public interface EnchantmentMenuAccessor {
    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> krp$getEnchantmentList(RegistryAccess registryAccess, ItemStack stack, int index, int cost);
}
