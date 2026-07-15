package com.kingdomrp.core.kingdom;

import com.kingdomrp.core.kingdom.ftb.FtbBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Серверные операции над королевствами: создание / расширение / роспуск.
 * Авторитет — {@link KingdomData}; force-load центрального чанка — через
 * NeoForge-тикет ({@link ServerLevel#setChunkForced}), независимо от FTB.
 * <p>Зеркалирование в FTB Teams/Chunks — шаг 5 (TODO-пометки).
 */
public final class KingdomManager {

    /** Радиус присоединяемых чанков вокруг центра: 2 → область 5×5 = 25 чанков. */
    public static final int RADIUS = 2;

    /** Палитра цветов королевства (заливка на карте FTB), случайный при создании. */
    public static final int[] COLOR_PALETTE = {
            0xE74C3C, 0xE67E22, 0xF1C40F, 0x2ECC71, 0x1ABC9C, 0x3498DB,
            0x9B59B6, 0xE84393, 0x16A085, 0x2980B9, 0x8E44AD, 0xD35400
    };

    private KingdomManager() {}

    /** Все чанки области 5×5 вокруг центра. */
    public static Set<ChunkPos> areaChunks(ChunkPos center) {
        Set<ChunkPos> out = new HashSet<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dz = -RADIUS; dz <= RADIUS; dz++)
                out.add(new ChunkPos(center.x + dx, center.z + dz));
        return out;
    }

    /** Свободна ли вся область под новое королевство (нет пересечений с чужими claim). */
    public static boolean isAreaFree(KingdomData data, ChunkPos center) {
        for (ChunkPos pos : areaChunks(center))
            if (data.byChunk(pos) != null) return false;
        return true;
    }

    /**
     * Создать королевство. Валидацию требований/подписей/неба выполняет вызывающий
     * (блок королевства). Здесь — только запись данных + force-load + FTB-зеркало.
     */
    public static Kingdom create(MinecraftServer server, ServerLevel level, BlockPos blockPos,
                                 String name, UUID king, Set<UUID> members, int color) {
        KingdomData data = KingdomData.get(server);

        ChunkPos center = new ChunkPos(blockPos);
        Kingdom k = new Kingdom(UUID.randomUUID(), name, king, center, blockPos, level.dimension());
        k.setColor(color);
        k.getMembers().addAll(members);
        for (UUID m : members) {
            ServerPlayer p = server.getPlayerList().getPlayer(m);
            if (p != null) k.setMemberLevel(m, totalLevel(p));
        }
        for (ChunkPos pos : areaChunks(center)) k.getClaims().add(pos.toLong());
        data.put(k);

        setForced(server, k, true);
        FtbBridge.onCreate(server, k);   // создаёт команду → пишет teamId в k
        com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.update(server, k);
        data.markDirty();
        return k;
    }

    /**
     * Расширить королевство на чанк newChunk (должен соседствовать с существующим claim
     * и быть свободным — проверяет вызывающий/этот метод).
     */
    public static boolean expand(MinecraftServer server, Kingdom k, ChunkPos newChunk) {
        KingdomData data = KingdomData.get(server);
        if (data.byChunk(newChunk) != null) return false;   // уже занят
        if (!isAdjacent(k, newChunk))       return false;

        k.getClaims().add(newChunk.toLong());
        data.markDirty();
        FtbBridge.onExpand(server, k, newChunk);
        return true;
    }

    /** Добавить жителя (принятое приглашение). */
    public static void addMember(MinecraftServer server, Kingdom k, ServerPlayer player) {
        if (!k.getMembers().add(player.getUUID())) return;
        k.setMemberLevel(player.getUUID(), totalLevel(player));
        KingdomData.get(server).markDirty();
        FtbBridge.joinMember(server, k, player);
        com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.apply(player, k);
        KingdomSync.broadcast(server, k);
    }

    /** Обновить снапшот суммарного уровня жителя (логин / левелап). */
    public static void refreshMemberLevel(ServerPlayer player) {
        Kingdom k = KingdomData.get(player.server).byPlayer(player.getUUID());
        if (k == null) return;
        k.setMemberLevel(player.getUUID(), totalLevel(player));
        KingdomData.get(player.server).markDirty();
    }

    private static int totalLevel(ServerPlayer player) {
        int sum = 0;
        for (int lvl : player.getData(com.kingdomrp.core.registry.KRPAttachments.PLAYER_DATA)
                .getSpecializationLevels().values()) sum += lvl;
        return sum;
    }

    /** Исключить жителя (кроме короля). */
    public static void removeMember(MinecraftServer server, Kingdom k, UUID uuid, String name) {
        if (k.isKing(uuid)) return;
        if (!k.getMembers().remove(uuid)) return;
        k.removeMemberLevel(uuid);
        KingdomData.get(server).markDirty();
        FtbBridge.kickMember(server, k, uuid, name);
        KingdomSync.broadcast(server, k);
        ServerPlayer kicked = server.getPlayerList().getPlayer(uuid);
        if (kicked != null) {
            com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.clear(kicked);   // снять баффы изгнанному
            KingdomSync.send(kicked);
        }
    }

    /** Роспуск: снять force-load, отклеймить, удалить данные, распустить FTB-команду. */
    public static void disband(MinecraftServer server, Kingdom k) {
        for (UUID m : k.getMembers()) {                    // снять баффы онлайн-жителям
            ServerPlayer p = server.getPlayerList().getPlayer(m);
            if (p != null) com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.clear(p);
        }
        setForced(server, k, false);
        FtbBridge.onDisband(server, k);

        ServerLevel level = server.getLevel(k.getDimension());
        if (level != null && level.getBlockState(k.getBlockPos()).is(
                com.kingdomrp.core.registry.KRPBlocks.KINGDOM_BLOCK.get())) {
            // Убрать хартию до сноса блока — иначе onRemove выронит её (переиспользование).
            if (level.getBlockEntity(k.getBlockPos())
                    instanceof com.kingdomrp.core.kingdom.block.KingdomBlockEntity be) {
                be.getCharterSlot().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
            }
            level.removeBlock(k.getBlockPos(), false);   // блок расходован
        }
        KingdomData.get(server).remove(k.getId());
    }

    /** Соседствует ли чанк ПО ГРАНИ (не по диагонали) с любым claim королевства. */
    public static boolean isAdjacent(Kingdom k, ChunkPos pos) {
        return k.owns(new ChunkPos(pos.x + 1, pos.z))
                || k.owns(new ChunkPos(pos.x - 1, pos.z))
                || k.owns(new ChunkPos(pos.x, pos.z + 1))
                || k.owns(new ChunkPos(pos.x, pos.z - 1));
    }

    /** Переустановить force-load центральных чанков всех королевств (вызов на старте сервера). */
    public static void reForceAll(MinecraftServer server) {
        KingdomData data = KingdomData.get(server);
        for (Kingdom k : data.all()) setForced(server, k, true);
    }

    private static void setForced(MinecraftServer server, Kingdom k, boolean forced) {
        ServerLevel level = server.getLevel(k.getDimension());
        if (level == null) return;
        ChunkPos c = k.getCenter();
        level.setChunkForced(c.x, c.z, forced);
    }

    @Nullable
    public static Kingdom byPlayer(MinecraftServer server, UUID player) {
        return KingdomData.get(server).byPlayer(player);
    }
}
