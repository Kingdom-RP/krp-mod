package com.kingdomrp.core.command;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.network.PacketHelper;
import com.kingdomrp.core.system.XPSystem;
import com.kingdomrp.core.KingdomRPCore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KingdomRPCore.MODID)
public class KRPCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("krp")
                        .then(Commands.literal("stats")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("Только для игроков"));
                                        return 0;
                                    }
                                    showStats(player);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("addxp")
                                .requires(src -> src.hasPermission(2)) // только оператор
                                .then(Commands.argument("path", IntegerArgumentType.integer(0, 4))
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                        ctx.getSource().sendFailure(Component.literal("Только для игроков"));
                                                        return 0;
                                                    }
                                                    Path path = Path.values()[IntegerArgumentType.getInteger(ctx, "path")];
                                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                                    XPSystem.giveXP(player, path, amount);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§aВыдано §f" + amount + " §aXP пути §f"
                                                                    + XPSystem.getPathName(path)), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("setlevel")
                                .requires(src -> src.hasPermission(2)) // только оператор
                                .then(Commands.argument("path", IntegerArgumentType.integer(0, 4))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                        ctx.getSource().sendFailure(Component.literal("Только для игроков"));
                                                        return 0;
                                                    }
                                                    Path path = Path.values()[IntegerArgumentType.getInteger(ctx, "path")];
                                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                                        while (data.getLevel(path) < level) {
                                                            data.addXP(path, data.getXPRequired(path));
                                                        }
                                                        PacketHelper.syncPlayer(player);
                                                    });
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§aПуть §f" + XPSystem.getPathName(path)
                                                                    + " §aустановлен на уровень §f" + level), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2)) // только оператор
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("Только для игроков"));
                                        return 0;
                                    }
                                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                        data.deserializeNBT(new net.minecraft.nbt.CompoundTag());
                                        PacketHelper.syncPlayer(player);
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aПрогресс сброшен"), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("debug")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                        for (Path path : Path.values()) {
                                            int spent     = data.getTotalSpentInPath(path);
                                            int available = data.getLevel(path) - spent;
                                            float multiplier = data.getXPMultiplier(path);
                                            player.sendSystemMessage(Component.literal(
                                                    "§e" + XPSystem.getPathName(path) +
                                                            " §7| ур: §f" + data.getLevel(path) +
                                                            " §7| xp: §f" + String.format("%.0f", data.getXP(path)) +
                                                            "§7/§f" + String.format("%.0f", data.getXPRequired(path)) +
                                                            " §7| очки: §f" + available +
                                                            " §7| множитель: §f" + String.format("%.0f%%", multiplier * 100)
                                            ));
                                        }
                                        player.sendSystemMessage(Component.literal("§6--- Специализации ---"));
                                        for (var entry : data.getSpecializationLevels().entrySet()) {
                                            player.sendSystemMessage(Component.literal(
                                                    "§f" + entry.getKey() + " §7= §f" + entry.getValue()
                                            ));
                                        }
                                    });
                                    return 1;
                                })
                        )
        );
    }

    private static void showStats(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            player.sendSystemMessage(Component.literal("§6========= Kingdom RP ========="));
            for (Path path : Path.values()) {
                int level     = data.getLevel(path);
                float xp      = data.getXP(path);
                float required = data.getXPRequired(path);
                String bar    = buildBar(xp, required, 10);
                player.sendSystemMessage(Component.literal(
                        String.format("§e%-12s §7Ур.§f%2d  §7[§a%s§7] §f%.0f§7/§f%.0f",
                                XPSystem.getPathName(path), level, bar, xp, required)
                ));
            }
            player.sendSystemMessage(Component.literal("§6==============================="));
        });
    }

    private static String buildBar(float current, float max, int length) {
        int filled = max > 0 ? (int) ((current / max) * length) : 0;
        return "█".repeat(filled) + "░".repeat(length - filled);
    }
}