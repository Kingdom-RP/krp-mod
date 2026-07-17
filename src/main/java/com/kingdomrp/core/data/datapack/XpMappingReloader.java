package com.kingdomrp.core.data.datapack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.entry.BlockEntry;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.entry.KillEntry;
import com.kingdomrp.core.data.entry.FoodTierEntry;
import com.kingdomrp.core.data.entry.BlockTierEntry;
import com.kingdomrp.core.data.entry.PlantEntry;
import com.kingdomrp.core.data.entry.AnimalTierEntry;
import com.kingdomrp.core.data.type.SpecRequirement;
import com.kingdomrp.core.data.map.BannedCraftMap;
import com.kingdomrp.core.data.map.tier.FoodTierMap;
import com.kingdomrp.core.data.map.tier.BlockTierMap;
import com.kingdomrp.core.data.map.tier.ItemUseTierMap;
import com.kingdomrp.core.data.map.tier.SmeltTierMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.map.tier.PlantTierMap;
import com.kingdomrp.core.data.map.tier.AnimalTierMap;
import com.kingdomrp.core.data.map.xp.BlockXPMap;
import com.kingdomrp.core.data.map.xp.FoodCookMap;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.xp.MobDamageMap;
import com.kingdomrp.core.data.map.xp.MobKillMap;
import com.kingdomrp.core.data.map.xp.MetalSmeltMap;
import com.kingdomrp.core.data.map.xp.NaturalSmeltMap;
import com.kingdomrp.core.data.map.xp.FishingXPMap;
import com.kingdomrp.core.data.map.xp.RepairXPMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Датапак-оверрайд XP-маппингов: правки баланса без рестарта сервера (через {@code
 * /reload}). Читает JSON из {@code data/<любой ns>/krp_xp/*.json} и перекрывает
 * BASE/compat-слои карт ({@code override}). BASE (хардкод+compat) остаётся фоллбэком —
 * оверрайд можно частичный, потеря данных исключена.
 * <p>
 * Файлы (ключ = ID предмета/блока/сущности):
 * <ul>
 *   <li>{@code block_xp.json}  — {@code {"minecraft:stone":{"path":"MINING","xp":1.0}}}</li>
 *   <li>{@code mob_kill.json}  — {@code {"minecraft:zombie":{"path":"WAR","xp":5.0}}}</li>
 *   <li>{@code mob_damage.json}— {@code {"minecraft:zombie":1.0}}</li>
 *   <li>{@code food_cook.json} — {@code {"minecraft:cooked_cod":1.0}}</li>
 *   <li>{@code item_craft.json}— {@code {"minecraft:stone_slab":{"path":"CRAFT","spec":"CRAFTSMAN","xp":0.5}}}</li>
 * </ul>
 * {@code path}/{@code spec} — имена enum (регистронезависимо). Неизвестные ID/поля —
 * WARN и пропуск.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class XpMappingReloader implements ResourceManagerReloadListener {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIR = "krp_xp";

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new XpMappingReloader());
    }

    @Override
    public void onResourceManagerReload(ResourceManager rm) {
        BlockXPMap.clearOverride();
        MobKillMap.clearOverride();
        MobDamageMap.clearOverride();
        FoodCookMap.clearOverride();
        ItemCraftMap.clearOverride();
        MetalSmeltMap.clearOverride();
        NaturalSmeltMap.clearOverride();
        FishingXPMap.clearOverride();
        RepairXPMap.clearOverride();
        FoodTierMap.clearOverride();
        BlockTierMap.clearOverride();
        ItemUseTierMap.clearOverride();
        SmeltTierMap.clearOverride();
        ItemCraftTierMap.clearOverride();
        PlantTierMap.clearOverride();
        AnimalTierMap.clearOverride();
        BannedCraftMap.clearOverride();

        int[] count = {0};

        forEach(rm, "food_cook.json", (id, el) -> {
            if (id.startsWith("#")) { FoodCookMap.overrideTag(itemTag(id), el.getAsFloat()); count[0]++; }
            else item(id, it -> { FoodCookMap.override(it, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "metal_smelt.json", (id, el) -> {
            if (id.startsWith("#")) { MetalSmeltMap.overrideTag(itemTag(id), el.getAsFloat()); count[0]++; }
            else item(id, it -> { MetalSmeltMap.override(it, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "natural_smelt.json", (id, el) -> {
            if (id.startsWith("#")) { NaturalSmeltMap.overrideTag(itemTag(id), el.getAsFloat()); count[0]++; }
            else item(id, it -> { NaturalSmeltMap.override(it, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "fishing.json", (id, el) -> {
            if (id.startsWith("#")) { FishingXPMap.overrideTag(itemTag(id), el.getAsFloat()); count[0]++; }
            else item(id, it -> { FishingXPMap.override(it, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "repair.json", (id, el) -> {
            if (id.startsWith("#")) { RepairXPMap.overrideTag(itemTag(id), el.getAsFloat()); count[0]++; }
            else item(id, it -> { RepairXPMap.override(it, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "mob_damage.json", (id, el) -> {
            if (id.startsWith("#")) { MobDamageMap.overrideTag(entityTag(id), el.getAsFloat()); count[0]++; }
            else entity(id, t -> { MobDamageMap.override(t, el.getAsFloat()); count[0]++; });
        });

        forEach(rm, "mob_kill.json", (id, el) -> {
            JsonObject o = el.getAsJsonObject();
            Path p = path(o); if (p == null) return;
            KillEntry k = new KillEntry(p, o.get("xp").getAsFloat());
            if (id.startsWith("#")) { MobKillMap.overrideTag(entityTag(id), k); count[0]++; }
            else entity(id, t -> { MobKillMap.override(t, k); count[0]++; });
        });

        forEach(rm, "block_xp.json", (id, el) -> {
            JsonObject o = el.getAsJsonObject();
            Path p = path(o); if (p == null) return;
            BlockEntry b = new BlockEntry(p, o.get("xp").getAsFloat());
            if (id.startsWith("#")) { BlockXPMap.overrideTag(blockTag(id), b); count[0]++; }
            else block(id, blk -> { BlockXPMap.override(blk, b); count[0]++; });
        });

        forEach(rm, "item_craft.json", (id, el) -> {
            JsonObject o = el.getAsJsonObject();
            Path p = path(o); if (p == null) return;
            CraftEntry c = new CraftEntry(p, spec(o), o.get("xp").getAsFloat());
            if (id.startsWith("#")) { ItemCraftMap.overrideTag(itemTag(id), c); count[0]++; }
            else item(id, it -> { ItemCraftMap.override(it, c); count[0]++; });
        });

        // ---- тир-гейты (SpecRequirement) ----
        forEach(rm, "food_tier.json", (id, el) -> {
            SpecRequirement r = specReq(el.getAsJsonObject()); if (r == null) return;
            FoodTierEntry e = new FoodTierEntry(r.spec(), r.level());
            if (id.startsWith("#")) { FoodTierMap.overrideTag(itemTag(id), e); count[0]++; }
            else item(id, it -> { FoodTierMap.override(it, e); count[0]++; });
        });

        forEach(rm, "block_tier.json", (id, el) -> {
            SpecRequirement r = specReq(el.getAsJsonObject()); if (r == null) return;
            BlockTierEntry e = new BlockTierEntry(r.spec(), r.level());
            if (id.startsWith("#")) { BlockTierMap.overrideTag(blockTag(id), e); count[0]++; }
            else block(id, b -> { BlockTierMap.override(b, e); count[0]++; });
        });

        forEach(rm, "item_use_tier.json", (id, el) -> {
            java.util.List<SpecRequirement> reqs = craftReqs(el);   // array-или-object → список (OR)
            if (reqs.isEmpty()) return;
            if (id.startsWith("#")) { ItemUseTierMap.overrideTag(itemTag(id), reqs); count[0]++; }
            else item(id, it -> { ItemUseTierMap.override(it, reqs); count[0]++; });
        });

        forEach(rm, "smelt_tier.json", (id, el) -> {
            SpecRequirement r = specReq(el.getAsJsonObject()); if (r == null) return;
            if (id.startsWith("#")) { SmeltTierMap.overrideTag(itemTag(id), r); count[0]++; }
            else item(id, it -> { SmeltTierMap.override(it, r); count[0]++; });
        });

        forEach(rm, "craft_tier.json", (id, el) -> {
            java.util.List<SpecRequirement> reqs = craftReqs(el); if (reqs.isEmpty()) return;
            if (id.startsWith("#")) { ItemCraftTierMap.overrideTag(itemTag(id), reqs); count[0]++; }
            else item(id, it -> { ItemCraftTierMap.override(it, reqs); count[0]++; });
        });

        forEach(rm, "plant_tier.json", (id, el) -> {
            JsonObject o = el.getAsJsonObject();
            SpecRequirement r = specReq(o); if (r == null) return;
            PlantEntry e = new PlantEntry(r.spec(), r.level());
            boolean grow = o.has("growable") && o.get("growable").getAsBoolean();
            if (id.startsWith("#")) { PlantTierMap.overrideTag(blockTag(id), e); count[0]++; }
            else block(id, b -> { PlantTierMap.override(b, e, grow); count[0]++; });
        });

        forEach(rm, "animal_tier.json", (id, el) -> {
            JsonObject o = el.getAsJsonObject();
            SpecRequirement r = specReq(o); if (r == null) return;
            float breed = o.has("breedXp") ? o.get("breedXp").getAsFloat() : 0f;
            AnimalTierEntry e = new AnimalTierEntry(r.spec(), r.level(), breed);
            if (id.startsWith("#")) { AnimalTierMap.overrideTag(entityTag(id), e); count[0]++; }
            else entity(id, t -> { AnimalTierMap.override(t, e); count[0]++; });
        });

        forEach(rm, "craft_ban.json", (id, el) -> {
            if (id.startsWith("#")) { BannedCraftMap.overrideTag(itemTag(id)); count[0]++; }
            else item(id, it -> { BannedCraftMap.override(it); count[0]++; });
        });

        if (count[0] > 0) LOGGER.info("KRP XP-датапак: применено {} оверрайдов", count[0]);
    }

    /** {spec,level} → SpecRequirement, либо null (WARN) при плохом spec. */
    private static SpecRequirement specReq(JsonObject o) {
        try {
            Spec s = Spec.valueOf(o.get("spec").getAsString().trim().toUpperCase(Locale.ROOT));
            return new SpecRequirement(s, o.get("level").getAsInt());
        } catch (Exception e) {
            LOGGER.warn("KRP датапак: неизвестный spec/level {}", o);
            return null;
        }
    }

    /** craft_tier: объект {spec,level} или массив таких → список требований. */
    private static java.util.List<SpecRequirement> craftReqs(com.google.gson.JsonElement el) {
        java.util.List<SpecRequirement> reqs = new java.util.ArrayList<>();
        if (el.isJsonArray()) {
            for (var x : el.getAsJsonArray()) {
                SpecRequirement r = specReq(x.getAsJsonObject());
                if (r != null) reqs.add(r);
            }
        } else {
            SpecRequirement r = specReq(el.getAsJsonObject());
            if (r != null) reqs.add(r);
        }
        return reqs;
    }

    /** Читает все {@code krp_xp/<file>} со всех датапаков (порядок стека — поздний перекрывает). */
    private void forEach(ResourceManager rm, String file, BiConsumer<String, JsonElement> consumer) {
        rm.listResources(DIR, p -> p.getPath().endsWith("/" + file) || p.getPath().equals(DIR + "/" + file))
                .forEach((loc, res) -> {
                    try (Reader reader = res.openAsReader()) {
                        JsonObject root = GSON.fromJson(reader, JsonObject.class);
                        if (root == null) return;
                        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                            try {
                                consumer.accept(e.getKey(), e.getValue());
                            } catch (Exception ex) {
                                LOGGER.warn("KRP XP-датапак: плохая запись {} в {}", e.getKey(), loc, ex);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("KRP XP-датапак: не прочитан {}", loc, ex);
                    }
                });
    }

    private static TagKey<net.minecraft.world.item.Item> itemTag(String key) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
    }

    private static TagKey<Block> blockTag(String key) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(key.substring(1)));
    }

    @SuppressWarnings("unchecked")
    private static TagKey<EntityType<?>> entityTag(String key) {
        return (TagKey<EntityType<?>>) (Object) TagKey.create(
                Registries.ENTITY_TYPE, ResourceLocation.parse(key.substring(1)));
    }

    private static Path path(JsonObject o) {
        try {
            return Path.valueOf(o.get("path").getAsString().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            LOGGER.warn("KRP XP-датапак: неизвестный path {}", o);
            return null;
        }
    }

    /** spec для item_craft: для XP не используется (важен path), дефолт CRAFTSMAN. */
    private static Spec spec(JsonObject o) {
        if (!o.has("spec")) return Spec.CRAFTSMAN;
        try {
            return Spec.valueOf(o.get("spec").getAsString().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Spec.CRAFTSMAN;
        }
    }

    private static void item(String id, java.util.function.Consumer<net.minecraft.world.item.Item> c) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).ifPresentOrElse(c,
                () -> LOGGER.warn("KRP XP-датапак: нет предмета {}", id));
    }

    private static void block(String id, java.util.function.Consumer<net.minecraft.world.level.block.Block> c) {
        BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).ifPresentOrElse(c,
                () -> LOGGER.warn("KRP XP-датапак: нет блока {}", id));
    }

    private static void entity(String id, java.util.function.Consumer<net.minecraft.world.entity.EntityType<?>> c) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).ifPresentOrElse(c,
                () -> LOGGER.warn("KRP XP-датапак: нет сущности {}", id));
    }
}
