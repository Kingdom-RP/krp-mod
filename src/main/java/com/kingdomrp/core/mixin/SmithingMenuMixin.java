package com.kingdomrp.core.mixin;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.map.BlacksmithTemperMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Кузнец (path {@link Path#CRAFT}) — незерит-апгрейд на кузнечном столе.
 * <p>
 * Незеритовый гир делается ТОЛЬКО на столе (не на верстаке), поэтому
 * {@code PlayerEvent.ItemCraftedEvent} для него не стреляет — обрабатываем здесь:
 * <ul>
 *   <li>access-гейт ур.{@value #NETHERITE_LEVEL} — результат не отдаём
 *       (createResult), нечего взять (дюп shift-click'ом исключён);</li>
 *   <li>XP за апгрейд ({@value #UPGRADE_XP});</li>
 *   <li>закалка результата по уровню (как у крафта на верстаке).</li>
 * </ul>
 * Трим брони (SmithingTrimRecipe) НЕ трогаем — отличаем по тому, что предмет
 * меняется на незеритовый (база ≠ результат).
 */
@Mixin(value = SmithingMenu.class, remap = false)
public class SmithingMenuMixin {

    @Unique private static final int   NETHERITE_LEVEL = 7;
    @Unique private static final float UPGRADE_XP      = 30f;

    @Unique private int krp$warnHash;

    @Unique
    private int krp$blacksmithLevel(Player player) {
        return player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.BLACKSMITH.id);
    }

    /** Это незерит-апгрейд (а не трим): предмет меняется на незеритовый. */
    @Unique
    private boolean krp$isUpgrade(Container inputs, ItemStack result) {
        if (result.isEmpty()) return false;
        if (BlacksmithTemperMap.get(result.getItem()) == null) return false;
        return inputs.getItem(1).getItem() != result.getItem();
    }

    /** Гейт на ВХОДЕ: не отдаём результат апгрейда, если уровень недостаточен. */
    @Inject(method = "createResult", at = @At("TAIL"), remap = false)
    private void krp$gateUpgrade(CallbackInfo ci) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return;
        SmithingMenu self = (SmithingMenu) (Object) this;
        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        int residx = self.getResultSlot();
        ItemStack result = self.getSlot(residx).getItem();
        if (!krp$isUpgrade(inputs, result)) return;

        Player player = ((ItemCombinerMenuAccessor) (Object) this).getPlayer();
        if (krp$blacksmithLevel(player) >= NETHERITE_LEVEL) return;

        self.getSlot(residx).set(ItemStack.EMPTY);

        int hash = Objects.hash(inputs.getItem(1).getItem(), inputs.getItem(2).getItem());
        if (hash != krp$warnHash) {
            krp$warnHash = hash;
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        "§c[Kingdom RP] Незерит-улучшение доступно с " + NETHERITE_LEVEL
                                + " уровня навыка «Кузнец»."));
            }
        }
        self.broadcastChanges();
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        if (!krp$isUpgrade(inputs, stack)) return;

        int level = krp$blacksmithLevel(player);
        if (KRPConfig.RESTRICTIONS_ENABLED.get() && level < NETHERITE_LEVEL) {
            stack.setCount(0); // подстраховка: гейт уже убрал результат в createResult
            ci.cancel();
            return;
        }

        // Прочность при незерит-апгрейде НЕ понижаем (vanilla переносит прочность
        // диаманта как есть) — закалка применяется только при крафте предмета.
        XPSystem.giveXP(serverPlayer, Path.CRAFT, UPGRADE_XP);
    }
}
