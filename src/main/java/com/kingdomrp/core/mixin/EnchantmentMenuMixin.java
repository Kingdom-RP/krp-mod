package com.kingdomrp.core.mixin;

import com.kingdomrp.core.data.EnchantTierMap;
import com.kingdomrp.core.data.EnchantXPMap;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.system.EnchantSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Зачарователь (path {@link Path#MAGIC}) — стол зачарования.
 * <p>
 * Гейт на HEAD {@code clickMenuButton}: число доступных вариантов по уровню
 * (ур.0–2 → 1, ур.3–4 → 2, ур.5+ → 3) и тир чар/книги ({@link EnchantTierMap}).
 * Запертое действие отменяется ДО траты ресурсов и не даёт XP.
 * <p>
 * Пост-обработка на RETURN (3-й return = «return true», ordinal=2) — после того
 * как ваниль наложила чары и списала лазурит/уровни: XP по ценности чар (и при
 * успехе, и при провале), ролл успеха, при провале — снятие чар + урон прочности
 * (книга пропадает), при успехе — активные бонусы (усиление чар, экономия
 * лазурита, сохранение опыта).
 */
@Mixin(value = EnchantmentMenu.class, remap = false)
public class EnchantmentMenuMixin {

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final public int[] costs;

    @Unique private int     krp$button;
    @Unique private int     krp$required;
    @Unique private int     krp$level;
    @Unique private boolean krp$wasBook;
    @Unique private Item    krp$lapisItem = Items.LAPIS_LAZULI;
    @Unique private Player  krp$player;

    /** Захват владельца меню (в конструкторе нет отдельного поля игрока). */
    @Redirect(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getEnchantmentSeed()I"),
            remap = false)
    private int krp$capturePlayer(Player player) {
        krp$player = player;
        return player.getEnchantmentSeed();
    }

    // Вокруг генерации списка чар выставляем уровень Зачарователя, чтобы
    // EnchantmentHelperMixin отфильтровал недоступные чары из пула стола.
    @Inject(method = "getEnchantmentList", at = @At("HEAD"), remap = false)
    private void krp$setTableLevel(net.minecraft.core.RegistryAccess registryAccess,
                                   ItemStack stack, int index, int cost,
                                   CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (krp$player != null) {
            EnchantSystem.setTableLevel(EnchantSystem.getEnchanterLevel(krp$player));
        }
    }

    @Inject(method = "getEnchantmentList", at = @At("RETURN"), remap = false)
    private void krp$clearTableLevel(net.minecraft.core.RegistryAccess registryAccess,
                                     ItemStack stack, int index, int cost,
                                     CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        EnchantSystem.clearTableLevel();
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$gateAndCapture(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer)) return;
        if (buttonId < 0 || buttonId >= this.costs.length) return;
        if (this.costs[buttonId] <= 0) return;

        ItemStack item = this.enchantSlots.getItem(0);
        if (item.isEmpty()) return;

        // Повторяем ванильную проверку доступности — чтобы не слать сообщение о
        // гейте там, где ваниль и так молча откажет (нет лазурита/уровней).
        int i = buttonId + 1;
        ItemStack lapis = this.enchantSlots.getItem(1);
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            if (lapis.isEmpty() || lapis.getCount() < i) return;
            if (player.experienceLevel < i || player.experienceLevel < this.costs[buttonId]) return;
        }

        int level = EnchantSystem.getEnchanterLevel(player);

        List<EnchantmentInstance> list = ((EnchantmentMenuAccessor) (Object) this)
                .krp$getEnchantmentList(player.registryAccess(), item, buttonId, this.costs[buttonId]);

        boolean isBook = item.is(Items.BOOK);
        int required = isBook ? EnchantSystem.BOOK_TABLE_LEVEL : 0;
        for (EnchantmentInstance ei : list) {
            required = Math.max(required, EnchantTierMap.requiredForEnchant(ei.enchantment, ei.level));
        }

        if (EnchantSystem.restrictionsEnabled()) {
            if (buttonId >= EnchantTierMap.slotCount(level)) {
                EnchantSystem.msg(player, "§c[Kingdom RP] Этот вариант заперт — прокачайте «Зачарователь».");
                cir.setReturnValue(false);
                return;
            }
            if (level < required) {
                if (isBook && level < EnchantSystem.BOOK_TABLE_LEVEL) {
                    EnchantSystem.msg(player, "§c[Kingdom RP] Зачарование книг доступно с уровня "
                            + EnchantSystem.BOOK_TABLE_LEVEL + " навыка «Зачарователь».");
                } else {
                    EnchantSystem.msg(player, "§c[Kingdom RP] Эти чары недоступны — прокачайте «Зачарователь».");
                }
                cir.setReturnValue(false);
                return;
            }
        }

        krp$button   = buttonId;
        krp$required = required;
        krp$level    = level;
        krp$wasBook  = isBook;
        krp$lapisItem = lapis.isEmpty() ? Items.LAPIS_LAZULI : lapis.getItem();
    }

    @Inject(method = "clickMenuButton", at = @At(value = "RETURN", ordinal = 2), remap = false)
    private void krp$onEnchantApplied(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack result = this.enchantSlots.getItem(0);
        if (result.isEmpty()) return;
        // Редкий случай: ваниль вернула true, но список чар был пуст (ничего не
        // наложилось) — не наказываем не-зачарованный предмет.
        if (EnchantmentHelper.getEnchantmentsForCrafting(result).isEmpty()) return;

        // XP по ценности наложенных чар — и при успехе, и при провале.
        float xp = EnchantXPMap.xp(result);
        if (xp > 0f) XPSystem.giveXP(serverPlayer, Path.MAGIC, xp);

        float chance = EnchantSystem.successChance(
                krp$level, krp$required, EnchantSystem.tableBaseChance());

        if (player.level().random.nextFloat() > chance) {
            if (krp$wasBook) {
                this.enchantSlots.setItem(0, ItemStack.EMPTY); // книга пропадает
            } else {
                EnchantmentHelper.setEnchantments(result, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                if (result.isDamageableItem()) {
                    int dmg = (int) (result.getMaxDamage() * EnchantSystem.failDurabilityFrac(krp$level));
                    result.setDamageValue(Math.min(result.getMaxDamage() - 1,
                            result.getDamageValue() + dmg));
                }
            }
            EnchantSystem.msg(player, "§c[Kingdom RP] Зачарование провалилось!");
            this.enchantSlots.setChanged();
            ((AbstractContainerMenu) (Object) this).broadcastChanges();
            return;
        }

        krp$applyBonuses(player, result);
        this.enchantSlots.setChanged();
        ((AbstractContainerMenu) (Object) this).broadcastChanges();
    }

    @Unique
    private void krp$applyBonuses(Player player, ItemStack result) {
        var rng = player.level().random;
        int level = krp$level;

        // Усиление чары (только для предметов — у книг чары в StoredEnchantments,
        // и EnchantmentHelper.setEnchantments писал бы не тот тег).
        if (!krp$wasBook) {
            float boost = level * EnchantSystem.ENCHANT_BOOST_PER_LEVEL;
            // Усиливаем чары по снимку, применяем через updateEnchantments.
            var current = EnchantmentHelper.getEnchantmentsForCrafting(result);
            java.util.Map<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>, Integer> ups =
                    new java.util.HashMap<>();
            for (var e : current.entrySet()) {
                int lvl = e.getIntValue();
                if (lvl < e.getKey().value().getMaxLevel() && rng.nextFloat() < boost) {
                    ups.put(e.getKey(), lvl + 1);
                }
            }
            if (!ups.isEmpty()) {
                EnchantmentHelper.updateEnchantments(result, mutable ->
                        ups.forEach(mutable::set));
            }
        }

        // Экономия лазурита: вернуть потраченные единицы.
        if (rng.nextFloat() < level * EnchantSystem.LAPIS_SAVE_PER_LEVEL) {
            int give = krp$button + 1;
            ItemStack lap = this.enchantSlots.getItem(1);
            if (lap.isEmpty()) {
                this.enchantSlots.setItem(1, new ItemStack(krp$lapisItem, give));
            } else if (lap.is(krp$lapisItem)) {
                lap.setCount(Math.min(lap.getMaxStackSize(), lap.getCount() + give));
            }
        }

        // Сохранение опыта: вернуть потраченные уровни.
        if (rng.nextFloat() < level * EnchantSystem.XP_SAVE_PER_LEVEL) {
            player.giveExperienceLevels(krp$button + 1);
        }
    }
}
