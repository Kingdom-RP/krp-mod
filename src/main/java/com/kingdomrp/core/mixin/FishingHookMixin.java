package com.kingdomrp.core.mixin;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FishingHook.class, remap = false)
public class FishingHookMixin {

    @Shadow
    private int timeUntilLured;

    @Shadow
    @Final
    private int lureSpeed;

    @Shadow
    @Final
    private int luck;

    @Shadow
    private boolean calculateOpenWater(BlockPos pos) {
        throw new AssertionError("mixin shadow stub"); // тело отбрасывается Mixin
    }

    /** Центр времени ожидания поклёвки при уровне 0 (тиков) ≈ ванильное среднее (17.5с). */
    private static final int LURED_CENTER_BASE = 350;

    /**
     * Линейное снижение центра ожидания за уровень (тиков).
     * Ур.10 = 350 − 190 = 160 тиков (8 с) — среднее без зачарования. С Lure I
     * (−3с) выходит ~5с, как и задумано «макс. уровень + зачарование на скорость».
     */
    private static final int LURED_CENTER_DROP_PER_LEVEL = 19;

    /** Нижняя граница центра (тиков) — 8 с среднего на макс. уровне. */
    private static final int LURED_CENTER_MIN = 160;

    /** Полуразмах случайного отклонения от центра (доля). 0.15 = ±15%. */
    private static final float LURED_JITTER_FRAC = 0.15f;

    /** Минимально возможное время клёва (тиков) = 3 с. Также страховка от перегенерации. */
    private static final int LURED_SAFETY_FLOOR = 60;

    /** Сколько тиков ожидания убирает 1 уровень чары Lure (3 с вместо ванильных 5 с). */
    private static final int LURE_TICKS_PER_LEVEL = 60;

    /** Сколько тиков Lure убирает в ванила (5 с) — столько уже вычтено из {@code value}. */
    private static final int VANILLA_LURE_TICKS_PER_LEVEL = 100;

    /** Уровень, с которого Рыбак игнорирует штраф за «закрытое небо». */
    private static final int OPEN_SKY_IGNORE_LEVEL = 5;

    /** Уровень, с которого Рыбак ловит сокровища без «открытой воды». */
    private static final int OPEN_WATER_IGNORE_LEVEL = 5;

    /**
     * Прибавка «luck» loot-контекста рыбалки за уровень (как Luck of the Sea).
     * Ур.10 ≈ +5 luck: вес хлама в ванильной таблице (`10 − 2·luck`) уходит в 0,
     * вес сокровищ (`5 + 2·luck`) растёт. Складывается с реальной чарой.
     */
    private static final float FISHER_LUCK_PER_LEVEL = 0.5f;

    /** Уровень специализации Рыбак у владельца снасти (0 если нет/не игрок). */
    private int krp$fishermanLevel() {
        Player player = ((FishingHook) (Object) this).getPlayerOwner();
        if (player == null) return 0;
        return player.getCapability(PlayerDataProvider.PLAYER_DATA)
                .map(data -> data.getSpecializationLevel(Spec.FISHERMAN.id))
                .orElse(0);
    }

    /**
     * Ускоренный клёв (фаза ожидания поклёвки, timeUntilLured).
     * <p>
     * Redirect на <b>запись</b> поля в строке {@code timeUntilLured -= lureSpeed*20*5}
     * (4-й PUTFIELD timeUntilLured в catchingFish, ordinal=3). Перехватываем именно
     * запись, а не чтение: {@code -=} грузит старое значение поля ДО вычисления
     * правой части, поэтому redirect на чтение был бы затёрт обратной записью.
     * <p>
     * {@code value} — ванильный итог регенерации (из него уже вычтено 5с/уровень
     * Lure). Чара Lure у нас ослаблена до 3с/уровень, поэтому ВЕЗДЕ возвращаем
     * разницу {@code (100−60)·lureSpeed = 40·lureSpeed} тиков.
     * <p>
     * Не-рыбак: пишем {@code value + 40·lureSpeed} (вся правка — это нерф Lure
     * до 3с; без чары = чистая ванила). Рыбак: задаём собственное время — центр,
     * линейно убывающий с уровнем ({@code 350 − level*19}, не ниже 160=8с) ±15%,
     * минус 3с/уровень Lure. На макс. уровне ~8с без чар / ~5с с Lure I, пол 3с.
     */
    @Redirect(
            method = "catchingFish",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;timeUntilLured:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 3,
                    remap = false
            ),
            remap = false
    )
    private void krp$setLured(FishingHook hook, int value) {
        // Нерф Lure до 3с/уровень: возвращаем разницу с ванильными 5с (для всех).
        int lureGiveBack = (VANILLA_LURE_TICKS_PER_LEVEL - LURE_TICKS_PER_LEVEL) * this.lureSpeed;

        int level = krp$fishermanLevel();
        if (level <= 0) {
            this.timeUntilLured = value + lureGiveBack; // ванила + ослабленный Lure
            return;
        }

        int center = Math.max(LURED_CENTER_MIN,
                LURED_CENTER_BASE - level * LURED_CENTER_DROP_PER_LEVEL);
        int jitter = Math.round(center * LURED_JITTER_FRAC);
        // Джиттер берём из value (он уже случаен — из nextInt(100,600)), без RandomSource
        int dev = jitter <= 0 ? 0 : Math.floorMod(value, 2 * jitter + 1) - jitter;
        int lureTicks = this.lureSpeed * LURE_TICKS_PER_LEVEL; // Lure 3с/уровень, поверх спеки
        this.timeUntilLured = Math.max(LURED_SAFETY_FLOOR, center + dev - lureTicks);
    }

    /**
     * Эффект Рыбака (с {@link #OPEN_SKY_IGNORE_LEVEL} ур.): игнорирование штрафа
     * за «закрытое небо». В catchingFish ванила с шансом 50% уменьшает множитель
     * {@code i} на тиках, где над поплавком не видно неба (`!canSeeSky`), из-за
     * чего таймеры рыбалки убывают медленнее. Для прокачанного рыбака считаем
     * небо всегда открытым → штраф не применяется. Единственный вызов
     * {@code canSeeSky} в методе, ordinal не нужен.
     */
    @Redirect(
            method = "catchingFish",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean krp$canSeeSky(Level level, BlockPos pos) {
        if (krp$fishermanLevel() >= OPEN_SKY_IGNORE_LEVEL) {
            return true; // игнор штрафа за закрытое небо
        }
        return level.canSeeSky(pos);
    }

    /**
     * Эффект Рыбака (с {@link #OPEN_WATER_IGNORE_LEVEL} ур.): ловля сокровищ без
     * «открытой воды». Сокровища ванилой разрешены только в открытой воде
     * (флаг {@code openWater}, выставляемый в tick() через {@code calculateOpenWater}
     * — область 5×5 вокруг поплавка должна быть водой/воздухом). Это ОТДЕЛЬНАЯ
     * от «открытого неба» механика. Для прокачанного рыбака считаем воду всегда
     * открытой → сокровища доступны и в прудах/у берега/с блоками рядом.
     * Единственный вызов {@code calculateOpenWater} в tick(), ordinal не нужен.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;calculateOpenWater(Lnet/minecraft/core/BlockPos;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean krp$calculateOpenWater(FishingHook hook, BlockPos pos) {
        if (krp$fishermanLevel() >= OPEN_WATER_IGNORE_LEVEL) {
            return true; // сокровища доступны без открытой воды
        }
        return this.calculateOpenWater(pos);
    }

    /**
     * Качество улова Рыбака. Redirect на чтение поля {@code luck} в
     * {@code retrieve()} (строка {@code withLuck((float)this.luck + ...)}).
     * Это обычное чтение (аргумент метода, не {@code -=}), поэтому redirect
     * чтения безопасен. Добавляем к luck бонус по уровню — больше сокровищ,
     * меньше хлама (как Luck of the Sea, складывается с реальной чарой).
     */
    @Redirect(
            method = "retrieve",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;luck:I",
                    opcode = Opcodes.GETFIELD,
                    remap = false
            ),
            remap = false
    )
    private int krp$luck(FishingHook hook) {
        int level = krp$fishermanLevel();
        if (level <= 0) return this.luck; // ванильное поведение
        return this.luck + Math.round(level * FISHER_LUCK_PER_LEVEL);
    }
}
