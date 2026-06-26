package com.kingdomrp.core.world;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.PlantEntry;
import com.kingdomrp.core.data.PlantTierMap;
import com.kingdomrp.core.specialization.Specialization;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class WorldEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Block placed = event.getPlacedBlock().getBlock();
        PlantEntry plant = PlantTierMap.get(placed);

        if (plant != null) {
            int specLevel = player.getData(KRPAttachments.PLAYER_DATA)
                    .getSpecializationLevel(plant.spec().id);

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

            // XP за посадку не даём (иначе посадка→ломка→пересадка = фарм XP):
            // награда Фермера — только сбор созревшего урожая.

            // Растущие культуры не трекаем как поставленные — сбор своей грядки
            // должен давать XP и бонусы Фермера.
            if (PlantTierMap.isGrowable(placed)) return;
        }

        PlacedBlockTracker.onPlaced(event.getPos());
    }
}
