package com.kingdomrp.core.kingdom.item;

import com.kingdomrp.core.kingdom.CharterData;
import com.kingdomrp.core.registry.KRPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Хартия королевства. Неподписанная — обычный предмет (как writable_book).
 * Подписанная несёт {@link CharterData} (имя, король, соподписанты).
 * Экран подписи/поддержки — клиент (шаг 6), логика взаимодействия — шаг 4/6.
 */
public class CharterItem extends Item {

    public CharterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // --- работа с компонентом ---

    @Nullable
    public static CharterData data(ItemStack stack) {
        return stack.get(KRPComponents.CHARTER.get());
    }

    public static boolean isSigned(ItemStack stack) {
        return stack.has(KRPComponents.CHARTER.get());
    }

    public static void set(ItemStack stack, CharterData data) {
        stack.set(KRPComponents.CHARTER.get(), data);
    }

    /**
     * ПКМ: неподписанная — открыть экран подписи (клиент, шаг 6);
     * подписанная не своя — поддержать (стать соподписантом).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CharterData d = data(stack);

        if (d == null) {
            // Класс-хук грузится только на клиенте (гейт level.isClientSide).
            if (level.isClientSide) {
                com.kingdomrp.core.client.KingdomClientHooks.openSignCharter();
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (d.king().equals(player.getUUID())) {
            warn(player, "kingdomrp.charter.self");
            return InteractionResultHolder.fail(stack);
        }
        if (d.hasCosigner(player.getUUID())) {
            warn(player, "kingdomrp.charter.already_cosigned");
            return InteractionResultHolder.fail(stack);
        }
        if (level.getServer() != null
                && com.kingdomrp.core.kingdom.KingdomData.get(level.getServer()).byPlayer(player.getUUID()) != null) {
            warn(player, "kingdomrp.charter.in_kingdom");
            return InteractionResultHolder.fail(stack);
        }
        set(stack, d.withCosigner(player.getUUID()));
        warn(player, "kingdomrp.charter.cosigned");
        return InteractionResultHolder.success(stack);
    }

    private static void warn(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW), true);
    }

    // --- отображение ---

    @Override
    public Component getName(ItemStack stack) {
        CharterData d = data(stack);
        if (d != null) return Component.literal(d.name());
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        CharterData d = data(stack);
        if (d == null) {
            tooltip.add(Component.translatable("kingdomrp.charter.unsigned")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("kingdomrp.charter.cosigners", d.cosigners().size())
                .withStyle(ChatFormatting.GRAY));
    }
}
