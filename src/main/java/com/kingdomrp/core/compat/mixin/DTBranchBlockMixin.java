package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * XP Лесоруба за рубку Dynamic Trees. Цель по СТРОКЕ (без зависимости от DT),
 * применяется только если DT загружен — см. {@link com.kingdomrp.core.compat.DTMixinPlugin}.
 * <p>
 * Хук — {@code BranchBlock.destroyBranchFromNode}: он вызывается ОДИН раз на каждую
 * фактическую валку (из {@code futureBreak} при рубке игроком; из shell-блока —
 * тоже через core-ветку). Возвращает {@code BranchDestructionData} с реальным
 * {@code woodVolume} = объём сваленного дерева (в брёвнах). Это чинит:
 * <ul>
 *   <li>trunk_shell (толстый ствол) — валит дерево через core-ветку → сюда попадает;</li>
 *   <li>двойной счёт — объём считается от места разруба вверх, долом остатка даёт
 *       объём ТОЛЬКО остатка, а не всё дерево снова.</li>
 * </ul>
 * Типы DT ({@code BranchDestructionData}, {@code NetVolumeNode.Volume}) читаем
 * рефлексией — их нет на compile-classpath. Объём берётся ДО умножения на Fortune
 * (оно в {@code futureBreak} уже после return).
 */
@Mixin(targets = "com.dtteam.dynamictrees.block.branch.BranchBlock", remap = false)
public abstract class DTBranchBlockMixin {

    /** XP за одно бревно объёма (паритет с ванильным `oak_log`). */
    private static final float XP_PER_LOG = 2f;
    /** Потолок XP за одно дерево (гиганты не абузятся). */
    private static final float MAX_XP = 40f;

    private static Field krp$woodVolumeField;
    private static Method krp$getVolumeMethod;

    @Inject(method = "destroyBranchFromNode", at = @At("RETURN"), remap = false)
    private void krp$treeXP(net.minecraft.world.level.Level level,
                            net.minecraft.core.BlockPos cutPos,
                            net.minecraft.core.Direction toolDir,
                            boolean wholeTree,
                            LivingEntity entity,
                            CallbackInfoReturnable<?> cir) {
        if (level.isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        Object data = cir.getReturnValue();
        if (data == null) return;

        try {
            if (krp$woodVolumeField == null) {
                krp$woodVolumeField = data.getClass().getField("woodVolume");
            }
            Object volume = krp$woodVolumeField.get(data);
            if (volume == null) return;
            if (krp$getVolumeMethod == null) {
                krp$getVolumeMethod = volume.getClass().getMethod("getVolume");
            }
            float logs = ((Number) krp$getVolumeMethod.invoke(volume)).floatValue();
            if (logs <= 0f) return;
            XPSystem.giveXP(player, Path.MINING, Math.min(MAX_XP, logs * XP_PER_LOG));
        } catch (Throwable ignored) {
            // DT сменил API — молча пропускаем (без спама/краша)
        }
    }
}
