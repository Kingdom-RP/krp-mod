package com.kingdomrp.core.kingdom.ftb;

import com.kingdomrp.core.kingdom.Kingdom;
import com.mojang.logging.LogUtils;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Мост к FTB Teams/Chunks. Наша система — авторитет; FTB держит команду (приват)
 * и клейм чанков (защита). Force-load центра идёт мимо FTB (NeoForge-тикет, шаг B).
 * Все вызовы мягко гейтятся {@link ModList#isLoaded} — без FTB мод работает без
 * командно-чанковой части.
 *
 * <p>Соподписанты должны быть онлайн в момент создания (проверяет вызывающий) —
 * присоединение через {@link PartyTeam#join}.
 */
public final class FtbBridge {

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean handlersRegistered = false;
    /** Пропуск блокировки клейма — только для наших собственных claim-вызовов. */
    private static boolean claimBypass = false;
    private static boolean unclaimBypass = false;   // пропуск блокировки отклейма (наш роспуск)

    private FtbBridge() {}

    /** Активен ли наш клейм — для миксина обхода лимита FTB (claim авторитетен у нас). */
    public static boolean isClaimBypass() { return claimBypass; }

    public static boolean teamsLoaded()  { return ModList.get().isLoaded("ftbteams"); }
    public static boolean chunksLoaded() { return ModList.get().isLoaded("ftbchunks"); }

    /** Запрет ручного создания команд + перехват ручного клейма. Вызов на старте сервера. */
    public static void init(MinecraftServer server) {
        if (teamsLoaded()) {
            FTBTeamsAPI.api().setPartyCreationFromAPIOnly(true);
        }
        registerHandlers();
    }

    private static void registerHandlers() {
        if (handlersRegistered || !chunksLoaded()) return;
        handlersRegistered = true;
        ClaimedChunkEvent.BEFORE_CLAIM.register((source, chunk) -> {
            if (claimBypass || source.hasPermission(2)) return CompoundEventResult.pass();
            return CompoundEventResult.interruptFalse(
                    ClaimResult.customProblem("kingdomrp.claim.blocked"));
        });
        // Блок ручного отклейма (клик по карте FTB) — снимать чанки королевства нельзя.
        // Оператор (в т.ч. /ftbchunks admin) не гейтится.
        ClaimedChunkEvent.BEFORE_UNCLAIM.register((source, chunk) -> {
            if (unclaimBypass || source.hasPermission(2)) return CompoundEventResult.pass();
            return CompoundEventResult.interruptFalse(
                    ClaimResult.customProblem("kingdomrp.claim.blocked"));
        });
    }

    /**
     * DEBUG: создать команду королевства от лица {@code actor} (не короля), заклеймить
     * область. Нужно для теста, когда король — фейк-офлайн, а приват должен реально
     * существовать (команда-владелец = actor, он же житель).
     */
    public static void onCreateDebug(MinecraftServer server, Kingdom k, ServerPlayer actor) {
        if (!teamsLoaded() || !chunksLoaded() || actor == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager()
                    .createPartyTeam(actor, k.getName(), "", Color4I.rgb(k.getColor()));
            k.setTeamId(team.getTeamId());
            team.setProperty(TeamProperties.COLOR, Color4I.rgb(k.getColor()));
            team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ALLOW_PVP, true);
            team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.CLAIM_VISIBILITY,
                    dev.ftb.mods.ftbteams.api.property.PrivacyMode.PUBLIC);
            team.markDirty();

            ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team);
            td.setExtraClaimChunks(td.getExtraClaimChunks() + k.getClaims().size());

            claimBypass = true;
            try {
                for (long l : k.getClaims())
                    FTBChunksAPI.api().claimAsPlayer(actor, k.getDimension(), new ChunkPos(l), false);
            } finally {
                claimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось создать debug-королевство {}", k.getName(), e);
        }
    }

    /** Создать команду королевства, присоединить онлайн-соподписантов, заклеймить 25 чанков. */
    public static void onCreate(MinecraftServer server, Kingdom k) {
        if (!teamsLoaded() || !chunksLoaded()) return;
        ServerPlayer king = server.getPlayerList().getPlayer(k.getKing());
        if (king == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager()
                    .createPartyTeam(king, k.getName(), "", Color4I.rgb(k.getColor()));
            k.setTeamId(team.getTeamId());
            team.setProperty(TeamProperties.COLOR, Color4I.rgb(k.getColor()));
            // Разрешаем pvp на клеймах FTB — иначе FTB Chunks сам режет урон; политику ведёт PvPSystem.
            if (chunksLoaded()) {
                team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ALLOW_PVP, true);
                // Приват виден всем (в т.ч. не-жителям) на карте/миникарте.
                team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.CLAIM_VISIBILITY,
                        dev.ftb.mods.ftbteams.api.property.PrivacyMode.PUBLIC);
            }
            team.markDirty();

            if (team instanceof PartyTeam party) {
                for (UUID member : k.getMembers()) {
                    if (member.equals(k.getKing())) continue;
                    ServerPlayer p = server.getPlayerList().getPlayer(member);
                    if (p != null) party.join(p);
                }
            }

            ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team);
            td.setExtraClaimChunks(td.getExtraClaimChunks() + k.getClaims().size());

            claimBypass = true;
            try {
                for (long l : k.getClaims())
                    FTBChunksAPI.api().claimAsPlayer(king, k.getDimension(), new ChunkPos(l), false);
            } finally {
                claimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось создать команду/клейм для королевства {}", k.getName(), e);
        }
    }

    /**
     * Заклеймить один расширенный чанк. Клеймим от лица ДЕЙСТВУЮЩЕГО игрока
     * (его команда = команда королевства), а не короля — иначе при офлайн-короле
     * чанк не заклеймился бы в FTB (данные наши обновлены, а защиты нет).
     */
    public static void onExpand(MinecraftServer server, Kingdom k, ServerPlayer actor, ChunkPos chunk) {
        if (!chunksLoaded() || k.getTeamId() == null || actor == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
            if (team != null) {
                ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team);
                td.setExtraClaimChunks(td.getExtraClaimChunks() + 1);
            }
            claimBypass = true;
            try {
                FTBChunksAPI.api().claimAsPlayer(actor, k.getDimension(), chunk, false);
            } finally {
                claimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось заклеймить чанк расширения для {}", k.getName(), e);
        }
    }

    /**
     * Переклеймить чанк, который уже числится за королевством в наших данных, но
     * в FTB отклеймлен (десинк после admin-отклейма). Клеймим от лица действующего
     * жителя (его команда = команда королевства). Аллованс не трогаем — слот
     * освободился при отклейме.
     */
    public static void reclaim(MinecraftServer server, Kingdom k, ServerPlayer actor, ChunkPos chunk) {
        if (!chunksLoaded() || k.getTeamId() == null) return;
        try {
            claimBypass = true;
            try {
                FTBChunksAPI.api().claimAsPlayer(actor, k.getDimension(), chunk, false);
            } finally {
                claimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось переклеймить чанк для {}", k.getName(), e);
        }
    }

    /**
     * Ресинк: заклеймить в FTB все чанки из {@code KingdomData}, которых там нет
     * (десинк после ранних багов с кольцом/лимитом). Клеймим от лица КОМАНДЫ
     * ({@code td.claim}), не игрока — чинит и при офлайн-жителях. Уже заклеймленные
     * → {@code ALREADY_CLAIMED} (no-op). Возвращает число доклеймленных чанков.
     */
    public static int resyncClaims(MinecraftServer server) {
        if (!teamsLoaded() || !chunksLoaded()) return 0;
        int claimed = 0;
        CommandSourceStack source = server.createCommandSourceStack();
        com.kingdomrp.core.kingdom.KingdomData data = com.kingdomrp.core.kingdom.KingdomData.get(server);
        claimBypass = true;
        try {
            for (Kingdom k : data.all()) {
                if (k.getTeamId() == null) continue;
                Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
                if (team == null) continue;
                ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team);
                for (long l : k.getClaims()) {
                    ChunkPos cp = new ChunkPos(l);
                    var res = td.claim(source, new ChunkDimPos(k.getDimension(), cp), false);
                    if (res instanceof dev.ftb.mods.ftbchunks.api.ClaimedChunk) claimed++;
                }
            }
        } catch (Exception e) {
            LOG.error("FTB: ресинк клеймов не удался", e);
        } finally {
            claimBypass = false;
        }
        return claimed;
    }

    /** Обновить цвет команды FTB (заливка на карте). */
    public static void applyColor(MinecraftServer server, Kingdom k) {
        if (!teamsLoaded() || k.getTeamId() == null) return;
        try {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId());
            if (team.isPresent()) {
                Color4I color = Color4I.rgb(k.getColor());
                team.get().setProperty(TeamProperties.COLOR, color);
                team.get().syncOnePropertyToAll(server, TeamProperties.COLOR, color);
                team.get().markDirty();
                // Форс-ресинк всех заклеймленных чанков — иначе карта перекрашивает частично.
                if (chunksLoaded()) {
                    ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team.get());
                    if (td instanceof ChunkTeamDataImpl impl) impl.syncChunksToAll(server);
                }
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось сменить цвет команды {}", k.getName(), e);
        }
    }

    /** Присоединить игрока к команде королевства (онлайн). */
    public static void joinMember(MinecraftServer server, Kingdom k, ServerPlayer player) {
        if (!teamsLoaded() || k.getTeamId() == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
            if (team instanceof PartyTeam party) party.join(player);
        } catch (Exception e) {
            LOG.error("FTB: не удалось добавить игрока в команду {}", k.getName(), e);
        }
    }

    /** Исключить игрока из команды королевства (в т.ч. офлайн). */
    public static void kickMember(MinecraftServer server, Kingdom k, java.util.UUID uuid, String name) {
        if (!teamsLoaded() || k.getTeamId() == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
            if (team instanceof PartyTeam party) {
                party.kick(server.createCommandSourceStack(),
                        java.util.List.of(new com.mojang.authlib.GameProfile(uuid, name)));
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось исключить игрока из команды {}", k.getName(), e);
        }
    }

    /** Переприменить свойства команды (pvp + публичная видимость привата). Для старых
     *  королевств на старте сервера — иначе созданные до фикса остаются невидимыми. */
    public static void applyTeamSettings(MinecraftServer server, Kingdom k) {
        if (!teamsLoaded() || !chunksLoaded() || k.getTeamId() == null) return;
        try {
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
            if (team == null) return;
            team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ALLOW_PVP, true);
            team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.CLAIM_VISIBILITY,
                    dev.ftb.mods.ftbteams.api.property.PrivacyMode.PUBLIC);
            team.markDirty();
        } catch (Exception e) {
            LOG.error("FTB: не удалось переприменить свойства команды {}", k.getName(), e);
        }
    }

    /** Снять клеймы всех чанков королевства и распустить команду FTB. */
    public static void onDisband(MinecraftServer server, Kingdom k) {
        if (!teamsLoaded() || k.getTeamId() == null) return;
        try {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId());
            if (team.isEmpty()) return;
            CommandSourceStack source = server.createCommandSourceStack();

            unclaimBypass = true;   // весь роспуск (отклейм + forceDisband) — мимо блокировки
            try {
                // forceDisband сам чанки не снимает — отклеймиваем явно.
                if (chunksLoaded()) {
                    ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team.get());
                    for (long l : k.getClaims()) {
                        ChunkPos cp = new ChunkPos(l);
                        td.unclaim(source, new ChunkDimPos(k.getDimension(), cp), false);
                    }
                }
                if (team.get() instanceof PartyTeam party) party.forceDisband(source);
            } finally {
                unclaimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось распустить команду королевства {}", k.getName(), e);
        }
    }
}
