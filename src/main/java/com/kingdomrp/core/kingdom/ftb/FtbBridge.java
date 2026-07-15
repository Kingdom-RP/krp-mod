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

    private FtbBridge() {}

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
            if (claimBypass) return CompoundEventResult.pass();
            return CompoundEventResult.interruptFalse(
                    ClaimResult.customProblem("kingdomrp.claim.blocked"));
        });
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
            if (chunksLoaded()) team.setProperty(dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ALLOW_PVP, true);
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

    /** Заклеймить один расширенный чанк. */
    public static void onExpand(MinecraftServer server, Kingdom k, ChunkPos chunk) {
        if (!chunksLoaded() || k.getTeamId() == null) return;
        ServerPlayer king = server.getPlayerList().getPlayer(k.getKing());
        if (king == null) return;   // клеймим от лица короля; офлайн — пропуск (данные уже наши)
        try {
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId()).orElse(null);
            if (team != null) {
                ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team);
                td.setExtraClaimChunks(td.getExtraClaimChunks() + 1);
            }
            claimBypass = true;
            try {
                FTBChunksAPI.api().claimAsPlayer(king, k.getDimension(), chunk, false);
            } finally {
                claimBypass = false;
            }
        } catch (Exception e) {
            LOG.error("FTB: не удалось заклеймить чанк расширения для {}", k.getName(), e);
        }
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

    /** Снять клеймы всех чанков королевства и распустить команду FTB. */
    public static void onDisband(MinecraftServer server, Kingdom k) {
        if (!teamsLoaded() || k.getTeamId() == null) return;
        try {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(k.getTeamId());
            if (team.isEmpty()) return;
            CommandSourceStack source = server.createCommandSourceStack();

            // forceDisband сам чанки не снимает — отклеймиваем явно.
            if (chunksLoaded()) {
                ChunkTeamData td = FTBChunksAPI.api().getManager().getOrCreateData(team.get());
                for (long l : k.getClaims()) {
                    ChunkPos cp = new ChunkPos(l);
                    td.unclaim(source, new ChunkDimPos(k.getDimension(), cp), false);
                }
            }
            if (team.get() instanceof PartyTeam party) party.forceDisband(source);
        } catch (Exception e) {
            LOG.error("FTB: не удалось распустить команду королевства {}", k.getName(), e);
        }
    }
}
