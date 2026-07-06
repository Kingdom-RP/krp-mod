package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Мягкая интеграция мода Handcrafted (мебель/утварь). Модовый контент гейтится
 * выше ванильного — с L5. Деревянная фурнитура (BlockItem с {@code mineable/axe})
 * → Плотник ({@link Spec#CARPENTER} L5, XP 3); всё прочее (керамика/текстиль/металл/
 * предметы-утварь) → Мастеровой ({@link Spec#CRAFTSMAN} L5, XP 2). Мебель не
 * взаимо-крафтится → XP безопасен.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class HandcraftedCompat {

    public static final String MODID = "handcrafted";

    private HandcraftedCompat() {}

    // На TagsUpdatedEvent, не на setup: mineable-теги привязываются к блокам
    // только после загрузки тегов из датапака.
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        register();
    }

    private static void register() {
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(MODID)) continue;

            boolean wood = item instanceof BlockItem bi
                    && bi.getBlock().builtInRegistryHolder().is(BlockTags.MINEABLE_WITH_AXE);

            Spec spec = wood ? Spec.CARPENTER : Spec.CRAFTSMAN;
            int level = 5;
            float xp = wood ? 3f : 2f;

            ItemCraftMap.addById(id.toString(), new CraftEntry(Path.CRAFT, spec, xp));
            ItemCraftTierMap.addById(id.toString(), new SpecRequirement(spec, level));
        }
    }
}
