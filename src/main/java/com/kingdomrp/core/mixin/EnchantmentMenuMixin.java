package com.kingdomrp.core.mixin;

import com.kingdomrp.core.data.map.tier.EnchantTierMap;
import com.kingdomrp.core.data.map.xp.EnchantXPMap;
import com.kingdomrp.core.data.type.Path;
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

import java.util.List;

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
                EnchantSystem.msg(player, "§c[Kingdom RP] Этот вариант зачарования вам пока не по силам. "
                        + "Повысьте уровень навыка «Зачарователь».");
                cir.setReturnValue(false);
                return;
            }
            if (level < required) {
                if (isBook && level < EnchantSystem.BOOK_TABLE_LEVEL) {
                    EnchantSystem.msg(player, "§c[Kingdom RP] Зачарование книг откроется на "
                            + EnchantSystem.BOOK_TABLE_LEVEL + "-м уровне навыка «Зачарователь».");
                } else {
                    EnchantSystem.msg(player, "§c[Kingdom RP] Эти чары слишком сильны для вашего уровня. "
                            + "Повысьте навык «Зачарователь».");
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

        // XP по ценности наложенных чар. Зачарование детерминированное (без RNG):
        // доступ решает тир-гейт выше (slotCount + required), при доступе — всегда успех.
        float xp = EnchantXPMap.xp(result);
        if (xp > 0f) XPSystem.giveXP(serverPlayer, Path.MAGIC, xp);

        krp$applyBonuses(player, result);
        this.enchantSlots.setChanged();
        ((AbstractContainerMenu) (Object) this).broadcastChanges();

        // Жертва крови: успешное зачарование забирает 2 ❤ (магия питается кровью чародея).
        // genericKill — чистый урон: игнорирует броню, зачарования, эффекты.
        serverPlayer.hurt(serverPlayer.damageSources().genericKill(), 4.0f);
        EnchantSystem.msg(serverPlayer, "§4Зачарование потребовало жертвы крови (−2 ❤).");

        // 5% — «пробуждение злых духов»: рядом спавнятся 3 вредителя, агрятся на игрока.
        krp$awakenEvilSpirits(serverPlayer);
    }

    @Unique
    private void krp$awakenEvilSpirits(ServerPlayer player) {
        var level = player.serverLevel();
        if (level.random.nextFloat() >= 0.05f) return;

        // Предупреждение сразу (звук + сообщение) — у игрока есть 3 сек среагировать.
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.65f);
        EnchantSystem.msg(player, "§5Вы случайно пробудили злых духов...");

        // Спавн через 3 сек (60 тик), в ~10 блоках от чародея.
        com.kingdomrp.core.system.DelayScheduler.schedule(player.server, 60,
                () -> krp$spawnVexes(player));
    }

    @Unique
    private void krp$spawnVexes(ServerPlayer player) {
        if (!player.isAlive()) return;
        var level = player.serverLevel();
        for (int i = 0; i < 3; i++) {
            net.minecraft.world.entity.monster.Vex vex =
                    net.minecraft.world.entity.EntityType.VEX.create(level);
            if (vex == null) continue;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 8.0 + level.random.nextDouble() * 4.0;   // ~8–12 блоков
            double vx = player.getX() + Math.cos(angle) * radius;
            double vz = player.getZ() + Math.sin(angle) * radius;
            vex.moveTo(vx, player.getY() + 1, vz, level.random.nextFloat() * 360f, 0f);
            vex.finalizeSpawn(level, level.getCurrentDifficultyAt(vex.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.EVENT, null);
            vex.setLimitedLife(20 * 60);                            // живут ~1 минуту
            vex.setTarget(player);                                  // сразу агрятся на чародея
            level.addFreshEntity(vex);
        }
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
