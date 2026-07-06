package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.map.BannedCraftMap;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Мягкая интеграция модов Macaw (мосты/окна/лестницы/крыши/заборы). Декор-блоки
 * классифицируются по инструменту добычи: {@code mineable/axe} → Плотник
 * ({@link Spec#CARPENTER}), иначе (камень/металл/стекло/верёвка) → Мастеровой
 * ({@link Spec#CRAFTSMAN}). Фонари ({@code bridge_lights}) пропускаются.
 * <p>
 * ⚠️ Мосты (bridges) — только ГЕЙТ, БЕЗ XP: формы взаимо-крафтятся (мост ↔ лестница
 * через {@code *_recycle}) → XP = бесконечный абуз. Остальные моды (окна/лестницы/
 * крыши/заборы) абуз-рецептов не имеют → дают XP.
 * <p>
 * Деревянные двери/люки/заборы Macaw дописаны в ванильные item-теги и уже покрыты
 * тег-правилами Плотника. Off-theme двери (гараж/металл/special) — бан.
 * mcwbyg/mcwbiomesoplenty добавляют варианты в те же теги → покрываются автоматически.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class MacawCompat {

    private MacawCompat() {}

    private static TagKey<Block> blockTag(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(id));
    }

    // На TagsUpdatedEvent, не на setup: mineable-теги грузятся из датапака и
    // привязываются к блокам только после загрузки тегов.
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        register();
    }

    private static void register() {
        // Модовый декор гейтится выше ванильного (от L6). Мосты — XP 0 (реверс-абуз
        // мост↔лестница), верх лестницы Плотника; фонари пропускаем.
        if (ModList.get().isLoaded("mcwbridges"))
            gateByMineable("mcwbridges", 7, 0f, blockTag("mcwbridges:bridge_lights"));
        if (ModList.get().isLoaded("mcwwindows"))
            gateByMineable("mcwwindows", 6, 4f, null);
        if (ModList.get().isLoaded("mcwstairs"))
            gateByMineable("mcwstairs", 6, 4f, null);
        if (ModList.get().isLoaded("mcwroofs"))
            gateByMineable("mcwroofs", 6, 4f, null);
        if (ModList.get().isLoaded("mcwfences"))
            gateByMineable("mcwfences", 6, 4f, null);
        if (ModList.get().isLoaded("mcwdoors"))
            banDoors("mcwdoors:garage_doors", "mcwdoors:metal_doors", "mcwdoors:special_doors");
    }

    /**
     * Гейт всех блоков мода по уровню спеца (Плотник если mineable/axe, иначе Мастеровой).
     * XP даём если {@code xp > 0} (0 — для мостов из-за реверс-абуза).
     * @param skipTag блоки этого тега пропустить (фонари), либо null.
     */
    private static void gateByMineable(String modid, int tier, float xp, TagKey<Block> skipTag) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(modid)) continue;
            var holder = block.builtInRegistryHolder();
            if (skipTag != null && holder.is(skipTag)) continue;

            Item item = block.asItem();
            if (item == Items.AIR) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            Spec spec = holder.is(BlockTags.MINEABLE_WITH_AXE) ? Spec.CARPENTER : Spec.CRAFTSMAN;

            ItemCraftTierMap.addById(itemId, new SpecRequirement(spec, tier));
            if (xp > 0f) ItemCraftMap.addById(itemId, new CraftEntry(Path.CRAFT, spec, xp));
        }
    }

    /** Бан крафта всех блоков перечисленных block-тегов (off-theme двери). */
    private static void banDoors(String... blockTagIds) {
        for (String tagId : blockTagIds) {
            TagKey<Block> tag = blockTag(tagId);
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!block.builtInRegistryHolder().is(tag)) continue;
                Item item = block.asItem();
                if (item != Items.AIR) {
                    BannedCraftMap.addById(BuiltInRegistries.ITEM.getKey(item).toString());
                }
            }
        }
    }
}
