package com.kingdomrp.core.world;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.data.PlantEntry;
import com.kingdomrp.core.data.PlantTierMap;
import com.kingdomrp.core.specialization.Specialization;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KingdomRPCore.MODID)
public class WorldEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Block placed = event.getPlacedBlock().getBlock();
        PlantEntry plant = PlantTierMap.get(placed);

        if (plant != null) {
            int specLevel = player.getCapability(PlayerDataProvider.PLAYER_DATA)
                    .map(data -> data.getSpecializationLevel(plant.spec().id))
                    .orElse(0);

            // Гейтинг посадки по уровню Фермера
            if (specLevel < plant.level()) {
                event.setCanceled(true);
                String specName = SpecializationRegistry.get(plant.spec().id)
                        .map(Specialization::getName)
                        .orElse(plant.spec().id);
                player.sendSystemMessage(Component.literal(
                        "§c[Kingdom RP] Для посадки этого растения прокачайте навык «"
                                + specName + "» до " + plant.level() + " уровня."
                ));
                return; // посадка отменена — клетку не трекаем
            }

            // XP за посадку (малая доля от добычи, путь Промысел)
            XPSystem.giveXP(player, Path.HARVEST, plant.plantXP());

            // Растущие культуры не помечаем как поставленные —
            // сбор своей грядки должен давать XP и бонусы Фермера
            if (PlantTierMap.isGrowable(placed)) return;
        }

        PlacedBlockTracker.onPlaced(event.getPos());
    }
}
