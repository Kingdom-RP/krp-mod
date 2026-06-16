package com.kingdomrp.core.mixin;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.EnchantTierMap;
import com.kingdomrp.core.data.EnchantXPMap;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.data.RepairXPMap;
import com.kingdomrp.core.data.Spec;
import com.kingdomrp.core.system.EnchantSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;

/**
 * Зачарователь (path {@link Path#MAGIC}) — наковальня, применение зачарованной
 * книги к предмету.
 * <p>
 * Access-гейт (книга = ур.5 + тир чар): результат не создаётся в
 * {@code createResult}, если уровень недостаточен — нечего взять, дюп
 * shift-click'ом исключён. Шанс успеха/провал — на {@code onTake} (обычный клик;
 * shift-взятие результата при применении книги блокируется в
 * {@link ItemCombinerMenuMixin}). Провал уничтожает ТОЛЬКО книгу, предмет
 * остаётся. Бонусы: скидка на уровни (возврат части), игнор «Too Expensive» с
 * ур.{@value EnchantSystem#ANVIL_IGNORE_COST_LEVEL}.
 */
@Mixin(value = AnvilMenu.class, remap = false)
public class AnvilMenuMixin {

    @Unique private boolean krp$success;
    @Unique private int     krp$cost;
    @Unique private int     krp$level;
    @Unique private int     krp$gateHash;

    @Unique
    private boolean krp$isBookApply(Container inputs) {
        ItemStack book = inputs.getItem(1);
        return book.is(Items.ENCHANTED_BOOK)
                && !EnchantmentHelper.getEnchantments(book).isEmpty();
    }

    @Unique
    private int krp$requiredFor(Container inputs) {
        Map<Enchantment, Integer> bookEnch = EnchantmentHelper.getEnchantments(inputs.getItem(1));
        return Math.max(EnchantSystem.ANVIL_LEVEL, EnchantTierMap.requiredForEnchants(bookEnch));
    }

    /** Гейт на ВХОДЕ: не создаём результат, если книга недоступна по уровню. */
    @Inject(method = "createResult", at = @At("TAIL"), remap = false)
    private void krp$gateBookApply(CallbackInfo ci) {
        if (!EnchantSystem.restrictionsEnabled()) return;
        AnvilMenu self = (AnvilMenu) (Object) this;
        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        if (!krp$isBookApply(inputs)) return;

        int residx = self.getResultSlot();
        if (self.getSlot(residx).getItem().isEmpty()) return;

        Player player = ((ItemCombinerMenuAccessor) (Object) this).getPlayer();
        int level = EnchantSystem.getEnchanterLevel(player);
        if (level >= krp$requiredFor(inputs)) return;

        self.getSlot(residx).set(ItemStack.EMPTY);
        self.setMaximumCost(0);

        int hash = Objects.hash(inputs.getItem(0).getItem(), inputs.getItem(1).getItem(),
                EnchantmentHelper.getEnchantments(inputs.getItem(1)));
        if (hash != krp$gateHash) {
            krp$gateHash = hash;
            EnchantSystem.msg(player,
                    "§c[Kingdom RP] Зачарование книгой на наковальне доступно с уровня "
                            + EnchantSystem.ANVIL_LEVEL + " навыка «Зачарователь».");
        }
        self.broadcastChanges();
    }

    /**
     * Кузнец — скидка на ремонт/объединение предметов на наковальне (не книга):
     * возврат части потраченных уровней {@code cost·min(0.5, level·0.05)}.
     * Применяется только при наличии второго входа (ремонт/объединение), не при
     * чистом переименовании.
     */
    @Unique
    private void krp$blacksmithRepairDiscount(Player player, Container inputs) {
        if (inputs.getItem(1).isEmpty()) return; // переименование без ремонта — без скидки
        AnvilMenu self = (AnvilMenu) (Object) this;
        int cost = self.getCost();
        if (cost <= 0) return;
        int level = player.getCapability(PlayerDataProvider.PLAYER_DATA)
                .map(d -> d.getSpecializationLevel(Spec.BLACKSMITH.id)).orElse(0);
        if (level <= 0) return;
        int refund = Math.round(cost * Math.min(0.5f, level * 0.05f));
        if (refund > 0 && !player.getAbilities().instabuild) {
            player.giveExperienceLevels(refund);
        }
    }

    /**
     * Кузнец — XP за починку предмета МАТЕРИАЛОМ (железный предмет + железо,
     * алмазный + алмаз и т.п.). Объединение двух предметов не считается (это
     * покрывает точильный камень). XP по материалу ({@link RepairXPMap}:
     * дерево/камень/золото/кожа=1, железо/кольчуга=2, алмаз=3, незерит=4).
     */
    @Unique
    private void krp$blacksmithRepairXP(ServerPlayer player, Container inputs) {
        ItemStack base = inputs.getItem(0);
        ItemStack material = inputs.getItem(1);
        if (base.isEmpty() || material.isEmpty()) return;
        if (!base.isDamageableItem() || !base.isDamaged()) return;
        if (!base.getItem().isValidRepairItem(base, material)) return; // только починка материалом
        XPSystem.giveXP(player, Path.CRAFT, RepairXPMap.get(base));
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$onTakeHead(Player player, ItemStack stack, CallbackInfo ci) {
        krp$success = false;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        if (!krp$isBookApply(inputs)) {
            krp$blacksmithRepairDiscount(player, inputs);     // Кузнец — скидка на ремонт
            krp$blacksmithRepairXP(serverPlayer, inputs);     // Кузнец — XP за починку материалом
            return;
        }

        int level = EnchantSystem.getEnchanterLevel(player);
        int required = krp$requiredFor(inputs);
        if (EnchantSystem.restrictionsEnabled() && level < required) {
            stack.setCount(0); // подстраховка: гейт уже убрал результат в createResult
            ci.cancel();
            return;
        }

        AnvilMenu self = (AnvilMenu) (Object) this;
        int cost = self.getCost();

        // XP по ценности чар книги — и при успехе, и при провале.
        float xp = EnchantXPMap.xp(EnchantmentHelper.getEnchantments(inputs.getItem(1)));
        if (xp > 0f) XPSystem.giveXP(serverPlayer, Path.MAGIC, xp);

        float chance = EnchantSystem.successChance(level, required, EnchantSystem.anvilBaseChance());
        if (player.level().random.nextFloat() > chance) {
            // Провал: сгорает ТОЛЬКО книга; предмет остаётся; уровни списываются.
            if (!player.getAbilities().instabuild) player.giveExperienceLevels(-cost);
            inputs.setItem(1, ItemStack.EMPTY);
            stack.setCount(0);
            EnchantSystem.msg(player,
                    "§c[Kingdom RP] Зачарование книгой провалилось! Книга уничтожена.");
            self.broadcastChanges();
            ci.cancel();
            return;
        }

        // Успех: ваниль завершит взятие; на TAIL вернём часть уровней (скидка).
        krp$success = true;
        krp$cost = cost;
        krp$level = level;
    }

    @Inject(method = "onTake", at = @At("TAIL"), remap = false)
    private void krp$onTakeTail(Player player, ItemStack stack, CallbackInfo ci) {
        if (!krp$success) return;
        krp$success = false;
        int refund = Math.round(krp$cost * (krp$level * EnchantSystem.ANVIL_DISCOUNT_PER_LEVEL));
        if (refund > 0 && !player.getAbilities().instabuild) {
            player.giveExperienceLevels(refund);
        }
    }

    // ===== Игнор «Too Expensive» (cost >= 40) с высокого уровня =====

    @Unique
    private boolean krp$enchanterIgnoresCost() {
        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        if (inputs.getItem(0).getCount() > 1) return false; // не снимаем кап для стака (анти-абуз)
        Player player = ((ItemCombinerMenuAccessor) (Object) this).getPlayer();
        return EnchantSystem.getEnchanterLevel(player) >= EnchantSystem.ANVIL_IGNORE_COST_LEVEL;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 1), remap = false)
    private int krp$cap1(int original) {
        return krp$enchanterIgnoresCost() ? Integer.MAX_VALUE : original;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2), remap = false)
    private int krp$cap2(int original) {
        return krp$enchanterIgnoresCost() ? Integer.MAX_VALUE : original;
    }
}
