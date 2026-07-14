package com.kingdomrp.core.command;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.entry.BlockEntry;
import com.kingdomrp.core.data.entry.KillEntry;
import com.kingdomrp.core.data.type.SpecRequirement;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.map.xp.BlockXPMap;
import com.kingdomrp.core.data.map.xp.MobKillMap;
import com.kingdomrp.core.data.map.xp.MobDamageMap;
import com.kingdomrp.core.data.map.xp.FishingXPMap;
import com.kingdomrp.core.data.map.xp.MetalSmeltMap;
import com.kingdomrp.core.data.map.xp.NaturalSmeltMap;
import com.kingdomrp.core.data.map.xp.RepairXPMap;
import com.kingdomrp.core.data.map.xp.FoodCookMap;
import com.kingdomrp.core.data.map.tier.FoodTierMap;
import com.kingdomrp.core.data.map.tier.BlockTierMap;
import com.kingdomrp.core.data.map.tier.ItemUseTierMap;
import com.kingdomrp.core.data.map.tier.SmeltTierMap;
import com.kingdomrp.core.data.map.tier.PlantTierMap;
import com.kingdomrp.core.data.map.tier.AnimalTierMap;
import com.kingdomrp.core.data.map.BannedCraftMap;
import com.kingdomrp.core.data.type.SpecializationRegistry;
import com.kingdomrp.core.network.PacketHelper;
import com.kingdomrp.core.system.XPSystem;
import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class KRPCommand {

    /** Автодополнение названий путей (craft, harvest, mining, war, magic). */
    private static final SuggestionProvider<CommandSourceStack> PATH_SUGGEST =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Path.values()).map(p -> p.name().toLowerCase()), builder);

    /** Автодополнение id специализаций (miner, lumberjack, farmer, ...). */
    private static final SuggestionProvider<CommandSourceStack> SPEC_SUGGEST =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Spec.values()).map(s -> s.id), builder);

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
                        // ===== setspec <spec> <level> [target] =====
                        .then(Commands.literal("setspec")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("spec", StringArgumentType.word()).suggests(SPEC_SUGGEST)
                                        .then(Commands.argument("level",
                                                IntegerArgumentType.integer(0, PlayerData.MAX_SPEC_LEVEL))
                                                .executes(ctx -> cmdSetSpec(ctx, false))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> cmdSetSpec(ctx, true)))
                                        )
                                )
                        )
                        // ===== xpaudit =====
                        .then(Commands.literal("xpaudit")
                                .requires(src -> src.hasPermission(2))
                                .executes(KRPCommand::cmdXpAudit)
                        )
                        // ===== reset [target] =====
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> cmdReset(ctx, false))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> cmdReset(ctx, true)))
                        )
                        // ===== reload =====
                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(KRPCommand::cmdReloadMappings)
                        )
                        // ===== exportdatapack =====
                        .then(Commands.literal("exportdatapack")
                                .requires(src -> src.hasPermission(2))
                                .executes(KRPCommand::cmdExportDatapack)
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

    /** Цель + путь для команд addxp/setlevel. */
    private record TargetPath(ServerPlayer player, Path path) {}

    /** Резолв игрока-цели и пути из аргументов, либо null с сообщением об ошибке. */
    private static TargetPath resolveTargetPath(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return null;
        Path path = parsePath(StringArgumentType.getString(ctx, "path"));
        if (path == null) {
            ctx.getSource().sendFailure(Component.literal("§cНеизвестный путь. Доступно: "
                    + pathNamesHint()));
            return null;
        }
        return new TargetPath(target, path);
    }

    private static int cmdAddXp(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        TargetPath tp = resolveTargetPath(ctx, hasTarget);
        if (tp == null) return 0;
        ServerPlayer target = tp.player();
        Path path = tp.path();
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        XPSystem.giveXP(target, path, amount);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aВыдано §f" + amount + " §aXP пути §f" + XPSystem.getPathName(path)
                        + " §aигроку §f" + target.getName().getString()), true);
        return 1;
    }

    private static int cmdSetLevel(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        TargetPath tp = resolveTargetPath(ctx, hasTarget);
        if (tp == null) return 0;
        ServerPlayer target = tp.player();
        Path path = tp.path();
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

    /** Спека по id (miner/lumberjack/...). null — не распознана. */
    private static Spec parseSpec(String s) {
        for (Spec spec : Spec.values()) {
            if (spec.id.equals(s.toLowerCase())) return spec;
        }
        return null;
    }

    // Прямая установка уровня специализации — тестовый шорткат в обход обычной
    // прокачки (K → уровень → кнопка спеки N раз). Очки пути НЕ тратятся честно,
    // но путь до нужного уровня подтягивается — эффекты, завязанные на pathLevel
    // (гейты BlockTierMap/PlantTierMap и т.п.), тоже открываются корректно.
    private static int cmdSetSpec(CommandContext<CommandSourceStack> ctx, boolean hasTarget)
            throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, hasTarget);
        if (target == null) return 0;
        Spec spec = parseSpec(StringArgumentType.getString(ctx, "spec"));
        if (spec == null) {
            ctx.getSource().sendFailure(Component.literal("§cНеизвестная специализация. Доступно: "
                    + specNamesHint()));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        Path path = SpecializationRegistry.get(spec.id)
                .map(s -> Path.values()[s.getPathIndex()])
                .orElse(null);

        withData(target, data -> {
            data.setSpecializationLevel(spec.id, level);
            if (path != null) {
                while (data.getLevel(path) < level) {
                    data.addXP(path, data.getXPRequired(path));
                }
            }
            PacketHelper.syncPlayer(target);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aСпециализация §f" + spec.id + " §aигрока §f" + target.getName().getString()
                        + " §aустановлена на уровень §f" + level), true);
        return 1;
    }

    private static String specNamesHint() {
        return String.join(", ", Arrays.stream(Spec.values()).map(s -> s.id).toList());
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

    // ===================== reload =====================
    // Анонс в общий чат → пауза 10с → перезагрузка датапаков (=/reload,
    // пересобирает XP-оверрайды через XpMappingReloader) → сообщение об итоге.

    private static final int RELOAD_DELAY_SEC = 10;

    private static int cmdReloadMappings(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§e[Kingdom RP] Через " + RELOAD_DELAY_SEC
                        + " секунд произойдёт обновление маппингов опыта..."), false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aПерезагрузка маппингов запланирована (" + RELOAD_DELAY_SEC + "с)."), true);

        java.util.concurrent.CompletableFuture
                .delayedExecutor(RELOAD_DELAY_SEC, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> server.execute(() -> doReloadMappings(server)));
        return 1;
    }

    private static void doReloadMappings(MinecraftServer server) {
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRunAsync(() -> server.getPlayerList().broadcastSystemMessage(Component.literal(
                        "§a[Kingdom RP] Маппинги опыта обновлены."), false), server)
                .exceptionally(e -> {
                    server.getPlayerList().broadcastSystemMessage(Component.literal(
                            "§c[Kingdom RP] Ошибка обновления маппингов: " + e.getMessage()), false);
                    return null;
                });
    }

    // ===================== exportdatapack =====================
    // Выгружает текущие значения XP-карт в валидный датапак. block_xp/item_craft —
    // BASE exact + тег-правила как #tag (не разворачиваются). Прочие — эффективные
    // концретные значения (BASE+compat+override).
    // <world>/datapacks/krp_balance/. Дальше правится вручную + /krp reload.
    // Правило-based карты (fishing/repair/brew) не выгружаются — вернули бы шум по
    // всем предметам; их override правится вручную по конкретному id.

    private static int cmdExportDatapack(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        java.nio.file.Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("krp_balance");
        java.nio.file.Path dir = packRoot.resolve("data/kingdomrpcore/krp_xp");
        var g = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(dir);
            Files.writeString(packRoot.resolve("pack.mcmeta"),
                    "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"Kingdom RP XP export\"\n  }\n}\n");

            Files.writeString(dir.resolve("food_cook.json"), g.toJson(itemFloatJson(FoodCookMap::get)));
            Files.writeString(dir.resolve("metal_smelt.json"), g.toJson(itemFloatJson(MetalSmeltMap::get)));
            Files.writeString(dir.resolve("natural_smelt.json"), g.toJson(itemFloatJson(NaturalSmeltMap::get)));

            JsonObject md = new JsonObject();
            MobDamageMap.exportEntries().forEach((t, xp) ->
                    md.addProperty(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(), xp));
            Files.writeString(dir.resolve("mob_damage.json"), g.toJson(md));

            JsonObject mk = new JsonObject();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                KillEntry e = MobKillMap.get(t);
                if (e == null) continue;
                mk.add(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(), pathXp(e.path().name(), e.xpReward()));
            }
            Files.writeString(dir.resolve("mob_kill.json"), g.toJson(mk));

            // BASE exact + тег-правила как #tag (не разворачиваем в конкретные предметы).
            JsonObject bx = new JsonObject();
            BlockXPMap.baseExact().forEach((b, e) ->
                    bx.add(BuiltInRegistries.BLOCK.getKey(b).toString(), pathXp(e.path().name(), e.xpReward())));
            for (var te : BlockXPMap.baseTags()) {
                bx.add("#" + te.getKey().location(), pathXp(te.getValue().path().name(), te.getValue().xpReward()));
            }
            Files.writeString(dir.resolve("block_xp.json"), g.toJson(bx));

            JsonObject ic = new JsonObject();
            ItemCraftMap.baseExact().forEach((it, e) -> {
                JsonObject o = pathXp(e.path().name(), e.xpReward());
                o.addProperty("spec", e.spec().name());
                ic.add(itemName(it), o);
            });
            for (var te : ItemCraftMap.baseTags()) {
                JsonObject o = pathXp(te.getValue().path().name(), te.getValue().xpReward());
                o.addProperty("spec", te.getValue().spec().name());
                ic.add("#" + te.getKey().location(), o);
            }
            Files.writeString(dir.resolve("item_craft.json"), g.toJson(ic));

            // ---- тир-гейты ----
            JsonObject ft = new JsonObject();
            FoodTierMap.baseEntries().forEach((it, e) -> ft.add(itemName(it), specLevel(e.spec().name(), e.level())));
            Files.writeString(dir.resolve("food_tier.json"), g.toJson(ft));

            JsonObject bt = new JsonObject();
            BlockTierMap.baseEntries().forEach((b, e) ->
                    bt.add(BuiltInRegistries.BLOCK.getKey(b).toString(), specLevel(e.spec().name(), e.level())));
            Files.writeString(dir.resolve("block_tier.json"), g.toJson(bt));

            JsonObject ut = new JsonObject();
            ItemUseTierMap.baseEntries().forEach((it, r) -> ut.add(itemName(it), specLevel(r.spec().name(), r.level())));
            Files.writeString(dir.resolve("item_use_tier.json"), g.toJson(ut));

            JsonObject st = new JsonObject();
            SmeltTierMap.baseEntries().forEach((it, r) -> st.add(itemName(it), specLevel(r.spec().name(), r.level())));
            Files.writeString(dir.resolve("smelt_tier.json"), g.toJson(st));

            JsonObject ct = new JsonObject();
            ItemCraftTierMap.baseExact().forEach((it, reqs) -> ct.add(itemName(it), specLevelArray(reqs)));
            for (var te : ItemCraftTierMap.baseTags()) {
                ct.add("#" + te.getKey().location(), specLevelArray(te.getValue()));
            }
            Files.writeString(dir.resolve("craft_tier.json"), g.toJson(ct));

            JsonObject pt = new JsonObject();
            PlantTierMap.baseEntries().forEach((b, e) -> {
                JsonObject o = specLevel(e.spec().name(), e.level());
                o.addProperty("growable", PlantTierMap.baseGrowable().contains(b));
                pt.add(BuiltInRegistries.BLOCK.getKey(b).toString(), o);
            });
            Files.writeString(dir.resolve("plant_tier.json"), g.toJson(pt));

            JsonObject at = new JsonObject();
            AnimalTierMap.baseEntries().forEach((t, e) -> {
                JsonObject o = specLevel(e.spec().name(), e.level());
                o.addProperty("breedXp", e.breedXP());
                at.add(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(), o);
            });
            Files.writeString(dir.resolve("animal_tier.json"), g.toJson(at));

            JsonObject cb = new JsonObject();
            BannedCraftMap.baseEntries().forEach(it -> cb.addProperty(itemName(it), true));
            Files.writeString(dir.resolve("craft_ban.json"), g.toJson(cb));
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("§cОшибка экспорта: " + e.getMessage()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aДатапак выгружен: §f" + packRoot + "\n§7Правь JSON → §f/krp reload"), false);
        return 1;
    }

    private static JsonObject itemFloatJson(Function<Item, Float> fn) {
        JsonObject o = new JsonObject();
        for (Item it : BuiltInRegistries.ITEM) {
            float v = fn.apply(it);
            if (v > 0) o.addProperty(itemName(it), v);
        }
        return o;
    }

    private static JsonObject pathXp(String path, float xp) {
        JsonObject o = new JsonObject();
        o.addProperty("path", path);
        o.addProperty("xp", xp);
        return o;
    }

    private static JsonObject specLevel(String spec, int level) {
        JsonObject o = new JsonObject();
        o.addProperty("spec", spec);
        o.addProperty("level", level);
        return o;
    }

    private static com.google.gson.JsonArray specLevelArray(List<SpecRequirement> reqs) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (SpecRequirement r : reqs) arr.add(specLevel(r.spec().name(), r.level()));
        return arr;
    }

    // ===================== xpaudit =====================
    // Дамп всех XP-маппингов в файл (баланс-сверка). Итерируем реестры и зовём
    // get() каждого маппинга — теги/fallback резолвятся штатно.

    private static int cmdXpAudit(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kingdom RP — XP audit\n=====================\n\n");

        auditCraft(sb);
        auditBlocks(sb);
        auditKills(sb);
        auditFloatMap(sb, "Fishing (FishingXPMap)", FishingXPMap::get);
        auditFloatMap(sb, "Metal smelt (MetalSmeltMap)", MetalSmeltMap::get);
        auditFloatMap(sb, "Natural smelt (NaturalSmeltMap)", NaturalSmeltMap::get);
        auditFloatMap(sb, "Repair (RepairXPMap)", it -> RepairXPMap.get(new ItemStack(it)));
        auditFloatMap(sb, "Cook (FoodCookMap)", FoodCookMap::get);

        java.nio.file.Path out = ctx.getSource().getServer()
                .getServerDirectory().resolve("krp_xpaudit.txt");
        try {
            Files.writeString(out, sb.toString());
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("§cОшибка записи: " + e.getMessage()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aXP-аудит записан: §f" + out.toString()), false);
        return 1;
    }

    private static String itemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** Тир доступа предмета для указанной спеки (ItemCraftTierMap), либо 0. */
    private static int craftTier(Item item, Spec spec) {
        List<SpecRequirement> reqs = ItemCraftTierMap.get(item);
        if (reqs == null) return 0;
        for (SpecRequirement r : reqs) {
            if (r.spec() == spec) return r.level();
        }
        return 0;
    }

    private static void auditCraft(StringBuilder sb) {
        EnumMap<Spec, List<Object[]>> bySpec = new EnumMap<>(Spec.class);
        for (Item it : BuiltInRegistries.ITEM) {
            CraftEntry e = ItemCraftMap.get(it);
            if (e == null) continue;
            bySpec.computeIfAbsent(e.spec(), k -> new ArrayList<>())
                    .add(new Object[]{itemName(it), e.xpReward(), craftTier(it, e.spec())});
        }
        sb.append("## Craft XP (ItemCraftMap) — по спеке, тир доступа × XP\n\n");
        for (var entry : bySpec.entrySet()) {
            List<Object[]> rows = entry.getValue();
            rows.sort((a, b) -> {
                int t = Integer.compare((int) a[2], (int) b[2]);
                return t != 0 ? t : Float.compare((float) b[1], (float) a[1]);
            });
            sb.append("### ").append(entry.getKey()).append("  (")
                    .append(rows.size()).append(")\n");
            // Свод по тирам: min/max XP — быстрая ловля инверсий
            Map<Integer, float[]> perTier = new java.util.TreeMap<>();
            for (Object[] r : rows) {
                int tier = (int) r[2];
                float xp = (float) r[1];
                float[] mm = perTier.computeIfAbsent(tier, k -> new float[]{Float.MAX_VALUE, 0f});
                mm[0] = Math.min(mm[0], xp);
                mm[1] = Math.max(mm[1], xp);
            }
            perTier.forEach((tier, mm) -> sb.append(String.format(
                    "  тир %d:  XP %.1f..%.1f\n", tier, mm[0], mm[1])));
            for (Object[] r : rows) {
                sb.append(String.format("    T%d  %6.1f  %s\n", (int) r[2], (float) r[1], r[0]));
            }
            sb.append("\n");
        }
    }

    private static void auditBlocks(StringBuilder sb) {
        EnumMap<Path, List<Map.Entry<String, Float>>> byPath = new EnumMap<>(Path.class);
        for (var block : BuiltInRegistries.BLOCK) {
            BlockEntry e = BlockXPMap.get(block);
            if (e == null) continue;
            byPath.computeIfAbsent(e.path(), k -> new ArrayList<>())
                    .add(Map.entry(BuiltInRegistries.BLOCK.getKey(block).toString(), e.xpReward()));
        }
        sb.append("## Block break XP (BlockXPMap) — по пути\n\n");
        for (var entry : byPath.entrySet()) {
            List<Map.Entry<String, Float>> rows = entry.getValue();
            rows.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            appendRows(sb, entry.getKey().name(), rows);
        }
    }

    private static void auditKills(StringBuilder sb) {
        List<Map.Entry<String, Float>> rows = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            KillEntry e = MobKillMap.get(type);
            if (e == null) continue;
            rows.add(Map.entry(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), e.xpReward()));
        }
        rows.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        sb.append("## Mob kill XP (MobKillMap)\n\n");
        appendRows(sb, "kills", rows);
    }

    private static void auditFloatMap(StringBuilder sb, String title, Function<Item, Float> fn) {
        List<Map.Entry<String, Float>> rows = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) {
            float v = fn.apply(it);
            if (v > 0) rows.add(Map.entry(itemName(it), v));
        }
        rows.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        sb.append("## ").append(title).append("\n\n");
        appendRows(sb, title, rows);
    }

    /** Печать группы: заголовок + count/min/max/avg + строки. */
    private static void appendRows(StringBuilder sb, String group, List<Map.Entry<String, Float>> rows) {
        if (rows.isEmpty()) {
            sb.append("### ").append(group).append("  (пусто)\n\n");
            return;
        }
        float min = Float.MAX_VALUE, max = 0f, sum = 0f;
        for (var r : rows) {
            min = Math.min(min, r.getValue());
            max = Math.max(max, r.getValue());
            sum += r.getValue();
        }
        sb.append(String.format("### %s  (n=%d, min=%.1f, max=%.1f, avg=%.2f)\n",
                group, rows.size(), min, max, sum / rows.size()));
        for (var r : rows) {
            sb.append(String.format("    %6.1f  %s\n", r.getValue(), r.getKey()));
        }
        sb.append("\n");
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
