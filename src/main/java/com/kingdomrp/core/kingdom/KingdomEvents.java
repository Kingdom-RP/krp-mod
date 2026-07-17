package com.kingdomrp.core.kingdom;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.block.KingdomBlockEntity;
import com.kingdomrp.core.kingdom.ftb.FtbBridge;
import com.kingdomrp.core.registry.KRPBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Серверные правила блока королевства:
 * <ul>
 *   <li>старт сервера: восстановить force-load центров + удалить «осиротевшие» королевства
 *       (блок пропал);</li>
 *   <li>ломание активного блока запрещено (до создания — можно);</li>
 *   <li>установка блока: требуется открытое небо над ним (как маяк);</li>
 *   <li>установка любого блока над активным блоком королевства запрещена.</li>
 * </ul>
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class KingdomEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // PVP-политику держит PvPSystem; движок должен разрешать урон (server.properties
        // pvp=false иначе режет всё до наших событий). «Вне территории» запрещаем сами.
        server.setPvpAllowed(true);
        FtbBridge.init(server);   // запрет ручных команд/клейма + перехватчик
        KingdomData data = KingdomData.get(server);

        // Осиротевшие (блок вырезан креативом/worldedit) → роспуск.
        List<Kingdom> orphans = new ArrayList<>();
        for (Kingdom k : data.all()) {
            ServerLevel level = server.getLevel(k.getDimension());
            if (level == null) continue;
            if (!level.getBlockState(k.getBlockPos()).is(KRPBlocks.KINGDOM_BLOCK.get()))
                orphans.add(k);
        }
        for (Kingdom k : orphans) KingdomManager.disband(server, k);

        KingdomManager.reForceAll(server);
        for (Kingdom k : data.all()) FtbBridge.applyTeamSettings(server, k);   // pvp + публ. видимость
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KingdomManager.refreshMemberLevel(player);   // снапшот уровня для потребления довольствия
            Kingdom k = KingdomData.get(player.server).byPlayer(player.getUUID());
            if (k != null) com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.apply(player, k);
            KingdomSync.send(player);
            com.kingdomrp.core.system.ArmorWeightHandler.recompute(player);   // штраф скорости брони (трансиент теряется на релоге)
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(KRPBlocks.KINGDOM_BLOCK.get())) return;
        LevelAccessor level = event.getLevel();
        if (level.isClientSide()) return;

        if (level.getBlockEntity(event.getPos()) instanceof KingdomBlockEntity be && be.isActive()) {
            event.setCanceled(true);
            warn(event.getPlayer(), "kingdomrp.block.protected");
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();
        Entity placer = event.getEntity();

        // (1) Блок королевства — только под открытым небом.
        if (placed.is(KRPBlocks.KINGDOM_BLOCK.get())) {
            if (!hasOpenSky(serverLevel, pos)) {
                event.setCanceled(true);
                if (placer instanceof Player p) warn(p, "kingdomrp.block.need_sky");
            }
            return;
        }

        // (2) Нельзя ставить блоки над активным блоком королевства.
        Kingdom k = KingdomData.get(serverLevel.getServer()).byChunk(new net.minecraft.world.level.ChunkPos(pos));
        if (k == null || !k.getDimension().equals(serverLevel.dimension())) return;
        BlockPos bp = k.getBlockPos();
        if (pos.getX() == bp.getX() && pos.getZ() == bp.getZ() && pos.getY() > bp.getY()) {
            event.setCanceled(true);
            if (placer instanceof Player p) warn(p, "kingdomrp.block.sky_blocked");
        }
    }

    /** Над позицией нет ни одного блока до потолка мира (строго воздух). */
    private static boolean hasOpenSky(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            m.setY(y);
            if (!level.getBlockState(m).isAir()) return false;
        }
        return true;
    }

    private static void warn(Player player, String key) {
        if (player != null)
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }
}
