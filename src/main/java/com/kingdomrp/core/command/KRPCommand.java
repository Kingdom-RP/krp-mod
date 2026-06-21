package com.kingdomrp.core.command;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.network.PacketHelper;
import com.kingdomrp.core.system.XPSystem;
import com.kingdomrp.core.KingdomRPCore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Arrays;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class KRPCommand {

    /** Автодополнение названий путей (craft, harvest, mining, war, magic). */
    private static final SuggestionProvider<CommandSourceStack> PATH_SUGGEST =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Path.values()).map(p -> p.name().toLowerCase()), builder);

    private static void withData(ServerPlayer player, java.util.function.Consumer<PlayerData> action) {
        action.accept(player.getData(KRPAttachments.PLAYER_DATA));
    }

    /** Путь по названию (craft/war/…) или по индексу (0–4). null — не распознан. */
    private static Path parsePath(String s) {
        if (s.matches("\\d+")) {
            int i = Integer.parseInt(s);
            return (i >= 0 && i < Path.values().length) ? Path.values()[i] : null;
        }
        try {
            return Path.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Игрок-цель: аргумент {@code target} если задан, иначе отправитель команды. */
    private static ServerPlayer resolveTarget(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        if (hasTarget) {
            return EntityArgument.getPlayer(ctx, "target");
        }
        if (ctx.getSource().getEntity() instanceof ServerPlayer self) {
            return self;
        }
        ctx.getSource().sendFailure(Component.literal(
                "§cУкажите игрока: команда вызвана не игроком (например, из консоли)."));
        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("krp")
                        // ===== stats [target] =====
                        .then(Commands.literal("stats")
                                .executes(ctx -> cmdStats(ctx, false))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> cmdStats(ctx, true)))
                        )
                        // ===== debug [target] =====
                        .then(Commands.literal("debug")
                                .executes(ctx -> cmdDebug(ctx, false))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> cmdDebug(ctx, true)))
                        )
                        // ===== addxp <path> <amount> [target] =====
                        .then(Commands.literal("addxp")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("path", StringArgumentType.word()).suggests(PATH_SUGGEST)
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                                .executes(ctx -> cmdAddXp(ctx, false))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> cmdAddXp(ctx, true)))
                                        )
                                )
                        )
                        // ===== setlevel <path> <level> [target] =====
                        .then(Commands.literal("setlevel")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("path", StringArgumentType.word()).suggests(PATH_SUGGEST)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> cmdSetLevel(ctx, false))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> cmdSetLevel(ctx, true)))
                                        )
                                )
                        )
                        // ===== reset [target] =====
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> cmdReset(ctx, false))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> cmdReset(ctx, true)))
                        )
        );
    }

    // ===================== обработчики =====================

    private static int cmdStats(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        showStats(ctx.getSource(), target);
        return 1;
    }

    private static int cmdDebug(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        CommandSourceStack src = ctx.getSource();
        withData(target, data -> {
            src.sendSuccess(() -> Component.literal("§6=== Kingdom RP debug: §f"
                    + target.getName().getString() + " §6==="), false);
            for (Path path : Path.values()) {
                int spent     = data.getTotalSpentInPath(path);
                int available = data.getLevel(path) - spent;
                float multiplier = data.getXPMultiplier(path);
                src.sendSuccess(() -> Component.literal(
                        "§e" + XPSystem.getPathName(path) +
                                " §7| ур: §f" + data.getLevel(path) +
                                " §7| xp: §f" + String.format("%.0f", data.getXP(path)) +
                                "§7/§f" + String.format("%.0f", data.getXPRequired(path)) +
                                " §7| очки: §f" + available +
                                " §7| множитель: §f" + String.format("%.0f%%", multiplier * 100)
                ), false);
            }
            src.sendSuccess(() -> Component.literal("§6--- Специализации ---"), false);
            for (var entry : data.getSpecializationLevels().entrySet()) {
                src.sendSuccess(() -> Component.literal(
                        "§f" + entry.getKey() + " §7= §f" + entry.getValue()), false);
            }
        });
        return 1;
    }

    private static int cmdAddXp(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        Path path = parsePath(StringArgumentType.getString(ctx, "path"));
        if (path == null) {
            ctx.getSource().sendFailure(Component.literal("§cНеизвестный путь. Доступно: "
                    + pathNamesHint()));
            return 0;
        }
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        XPSystem.giveXP(target, path, amount);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aВыдано §f" + amount + " §aXP пути §f" + XPSystem.getPathName(path)
                        + " §aигроку §f" + target.getName().getString()), true);
        return 1;
    }

    private static int cmdSetLevel(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        Path path = parsePath(StringArgumentType.getString(ctx, "path"));
        if (path == null) {
            ctx.getSource().sendFailure(Component.literal("§cНеизвестный путь. Доступно: "
                    + pathNamesHint()));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        withData(target, data -> {
            while (data.getLevel(path) < level) {
                data.addXP(path, data.getXPRequired(path));
            }
            PacketHelper.syncPlayer(target);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aПуть §f" + XPSystem.getPathName(path) + " §aигрока §f"
                        + target.getName().getString() + " §aустановлен на уровень §f" + level), true);
        return 1;
    }

    private static int cmdReset(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        withData(target, data -> {
            data.deserializeNBT(target.registryAccess(), new net.minecraft.nbt.CompoundTag());
            PacketHelper.syncPlayer(target);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aПрогресс игрока §f" + target.getName().getString() + " §aсброшен"), true);
        return 1;
    }

    private static String pathNamesHint() {
        return String.join(", ", Arrays.stream(Path.values()).map(p -> p.name().toLowerCase()).toList());
    }

    private static void showStats(CommandSourceStack src, ServerPlayer player) {
        withData(player, data -> {
            src.sendSuccess(() -> Component.literal("§6===== Kingdom RP: §f"
                    + player.getName().getString() + " §6====="), false);
            for (Path path : Path.values()) {
                int level     = data.getLevel(path);
                float xp      = data.getXP(path);
                float required = data.getXPRequired(path);
                String bar    = buildBar(xp, required, 10);
                src.sendSuccess(() -> Component.literal(
                        String.format("§e%-12s §7Ур.§f%2d  §7[§a%s§7] §f%.0f§7/§f%.0f",
                                XPSystem.getPathName(path), level, bar, xp, required)), false);
            }
            src.sendSuccess(() -> Component.literal("§6==============================="), false);
        });
    }

    private static String buildBar(float current, float max, int length) {
        int filled = max > 0 ? (int) ((current / max) * length) : 0;
        return "█".repeat(filled) + "░".repeat(length - filled);
    }
}
