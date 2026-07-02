# Kingdom RP — architecture, notes

## Stack

- Minecraft **1.21.1**, **NeoForge 21.1.233** (`net.neoforged:neoforge:21.1.233`;
  pkgs `net.neoforged.neoforge.*` / `net.neoforged.fml.*` / `net.neoforged.bus.api.*`)
- **Java 21**, IntelliJ IDEA
- Build: **ModDevGradle** (`net.neoforged.moddev` 2.0.141), **Gradle 8.14**. Runtime on
  Mojmap (official mappings) — NO reobf.
- Mixin — no MixinGradle (no refmap, Mojmap runtime), shipped by NeoForge. All mixins
  `remap = false`, `compatibilityLevel JAVA_21`. Mod ID `kingdomrpcore`, main class
  `KingdomRPCore` (ctor `@Mod(IEventBus, ModContainer)`).
- 1.21 infra: player data = **Data Attachment** (`registry/KRPAttachments`), net =
  **PayloadRegistrar** (`CustomPacketPayload` + `StreamCodec`), config = **ModConfigSpec**,
  HUD = **LayeredDraw**, magic = **Data Components** (`PotionContents`/`ItemEnchantments`).

> **Mixin config registration.** `kingdomrpcore.mixins.json` loaded **natively via
> `[[mixins]]` `config="..."` in `neoforge.mods.toml`** (1.21 supported; 1.20.1
> `MixinConfigs` manifest hack removed). Verify in `run/logs/debug.log`:
> `Registering mixin config` + `Mixing <X> into <target>`. ⚠️ Inject targets checked at
> RUNTIME on target-class load (some lazy) — after mixin edits need `runClient` + open the
> menu, not just `build`.

---

## Package structure

```
com.kingdomrp.core
├── capability/         PlayerData (INBTSerializable), PlayerDataEvents (login-sync)
├── registry/           KRPAttachments (Data Attachment), KRPEffects (mob effects)
├── client/
│   ├── screen/          PathScreen, SpecializationScreen
│   └── KeyBindings, ClientEvents, XPHudOverlay (LayeredDraw)
├── command/             KRPCommand (/krp ...)
├── config/              KRPConfig (ModConfigSpec)
├── compat/              soft mod integrations (FarmersDelight/Tide/Backpacks/DynamicTrees) + mixin/
├── data/                all mappings + enums (below)
├── mixin/                mixin classes
├── network/              NetworkHandler (PayloadRegistrar), CustomPacketPayload packets
├── specialization/      Specialization, SpecializationRegistry
├── system/               XPSystem, RestrictionSystem, SpecializationEffects, MagicSystem, CookSystem
├── util/                 ScalingFormula
└── world/                PlacedBlockTracker
```

---

## Key data types (`data/`)

- `Path` enum — 5 paths: CRAFT, HARVEST, MINING, WAR, MAGIC (field `index`)
- `Spec` enum — all specs, field `id` (string for serialization)
- `BlockEntry(Path, float xpReward)` — break XP → `BlockXPMap`
- `BlockTierEntry(Spec, int level)` — spec-level req to mine block → `BlockTierMap`
- `AnimalTierEntry(Spec, int level, float breedXP)` → `AnimalTierMap` — Farmer-level gate
  on breeding + breed XP. Key = `EntityType`. Same level gates milking/shearing
- `PlantEntry(Spec, int level, float plantXP)` → `PlantTierMap` — Farmer-level gate on
  **planting** + plant XP. Key = block placed on plant (`EntityPlaceEvent` result).
  `PlantTierMap.isGrowable(block)` — growing crops NOT marked in `PlacedBlockTracker`
- `OreDropMap` — Set<Block>, which ore blocks (Miner bonus drop)
- `KillEntry(Path, float xpReward)` — kill XP → `MobKillMap`
- `FishingXPMap` — catch XP (`get(item)`) + treasure flag (`isTreasure(item)`), path=HARVEST
- `MobDamageMap` — damage-dealt XP (by tier, separate from kill)
- `CraftEntry(Path, Spec, float xpReward)` → `ItemCraftMap`. Craft ALWAYS succeeds —
  `baseChance/maxBonus/k` removed (unused). Progression = access ladders
  (`ItemCraftTierMap`/`FoodTierMap`) + skill effects
- `SpecRequirement(Spec, int level)` — gate by SPECIFIC spec level (not path). Replaced old
  `TierRequirement(Path, level)`.
  - `ItemCraftTierMap` — `Map<Item, List<SpecRequirement>>`, **craft** gate, ALL must pass
    (dual-spec: golden carrot/apple = Alchemist 3 + Enchanter 3). Spec = the profession
    making the item.
  - `ItemUseTierMap` — `Map<Item, SpecRequirement>` (one req), **wear/use** gate: armor+melee
    = Warrior, crossbow = Archer.
  - One item can be in both with different specs (netherite sword: craft Blacksmith 7, wear Warrior 7).

---

## Leveling (PlayerData)

- `pathXP[Path]`, `pathLevel[Path]` — float/int arrays by path index
- `specializationLevels: Map<String specId, Integer level>`
- `MAX_SPEC_LEVEL = 10` — cap per single spec
- `getMaxLevel(path) = SpecializationRegistry.getCount(path) * MAX_SPEC_LEVEL`
- `getXPRequired(path) = 100 * 1.5^level` (config: `baseXP`, `xpCurve`)
- `getXPMultiplier(path)` — priority penalty: `priority = (#paths with level > current) + 1`,
  `multiplier = 1/priority` (1st path=100%, 2nd=50%, 3rd=33%). Applied **at XP-award time**,
  not retroactively (no abuse via hoard-level-then-invest)
- Spec points: 1 path level = 1 point, 1 spec level = 1 point. Freely distributed among
  same-path specs.

---

## XP awarding (XPSystem)

Single point: `XPSystem.giveXP(player, Path, amount)` — applies `getXPMultiplier`, awards,
syncs, sends HUD progress bar (`PacketHelper.sendXPBar` → `XPGainPacket` → `client/XPHudOverlay`)
+ level-up message.

**HUD progress bar** (`client/XPHudOverlay`, `IGuiOverlay` over `VanillaGuiOverlay.HOTBAR`):
top-of-screen on any XP award, progress to next level of current path. Drawn procedurally
(vanilla `icons.png` not used — its green pixels can't be tinted blue/yellow, `setColor` only
multiplies). Light-blue on normal award, **yellow + `SoundEvents.PLAYER_LEVELUP`** on level-up.
Single static state — max one bar; new packet overwrites. Held `DISPLAY_MS` (3s) with fade.
Data (`currentXP`/`requiredXP`/`level`/`leveledUp`) sent by server in `giveXP` — client doesn't
recompute from PlayerData sync.

XP sources:
- `onBlockBreak` → `BlockXPMap` (with `PlacedBlockTracker` check)
- `onLivingDeath` → `MobKillMap`
- `onLivingHurt` → `MobDamageMap` (dmg to mob) + 1 XP/tick for player taking dmg
- `onArrowHit` (ProjectileImpactEvent) → 2 XP per arrow hit
- `onCraft` (`ItemCraftedEvent`) → `ItemCraftMap` (always succeeds; gated recipes can't be
  taken — `SlotMixin.mayPickup`)
- `onFishing` → fixed XP, more for rare catch
- Brewing/enchanting/anvil — see `MagicSystem` + mixins, award `Path.MAGIC`

---

## Restrictions (RestrictionSystem + SpecializationEffects)

- `RestrictionSystem.getCraftRequirements(stack)` → `List<SpecRequirement>` (`ItemCraftTierMap`);
  `getUseRequirement(stack)` → `SpecRequirement` (`ItemUseTierMap`). Both by SPEC level
  (`data.getSpecializationLevel`), not path.
- Use/wear without level — canceled, armor removed (`LivingEquipmentChangeEvent`), chat msg
- Craft without level — result VISIBLE but pickup blocked (`SlotMixin` on `Slot.mayPickup`,
  `isCraftBlocked`) + chat warn on attempt. Dual-spec: ALL reqs checked (`isCraftBlocked` true
  if any fails)
- Mine block without spec level (`BlockTierMap`) — break canceled (`event.setCanceled(true)`),
  no start message
- `KRPConfig.RESTRICTIONS_ENABLED` — global switch

---

## SpecializationEffects — format

One `@SubscribeEvent` per Forge event, calls private per-spec check methods:

```java
@SubscribeEvent
public static void onBlockBreak(BlockEvent.BreakEvent event) {
    ... common checks (client, PlacedBlockTracker) ...
    checkTierRestriction(...);  // may .setCanceled() and return
    checkMiner(...);
    checkLumberjack(...);
    checkFarmer(...);
}
```

Same for `PlayerEvent.BreakSpeed`, `LivingHurtEvent`, `PlayerEvent.ItemCraftedEvent`,
`ArrowLooseEvent`.

---

## Specs (full status)

### Mining path — DONE ✅

**Miner**
- XP: stone/ores via `BlockXPMap`, harder/rarer → more (deepslate variants cost more)
- Access (`BlockTierMap`, spec=MINER): L1 coal+copper, L2 iron+gold, L3 redstone+lapis+emerald,
  L4 diamond+obsidian(+crying), L5 nether quartz/gold + ancient debris
- Mine speed: `+5%/level` (linear, `PlayerEvent.BreakSpeed`)
- Ore double-drop: `level*0.025` (2.5%/lvl, max 25% @L10), only `OreDropMap` blocks

**Lumberjack**
- Log-chop XP: L0 (oak/birch/spruce)=2, L2 (jungle/acacia/dark_oak)=4, L4 (mangrove/cherry)=8,
  L6 (crimson/warped/mushroom)=16
- Strip-log XP (RMB axe) — path CRAFT (Carpenter), see Carpenter section
- Access (`BlockTierMap`, spec=LUMBERJACK) — matches XP tiers
- Chop speed: `+5%/lvl` — logs AND worked wood (`isWorkedWood`: block whose item Carpenter
  crafts — planks/furniture/doors/fences; set tied to `ItemCraftMap` spec=CARPENTER, no dup list)
  AND Dynamic Trees branch blocks (`isDtBranch`, matched by class package)
- Axe durability save: `level*0.05` (5%/lvl, max 50%). Damage rollback via `server.execute`
  (next tick) clamped `Math.min(before, ...)` — `BreakEvent` fires before tool dmg; naive
  `setDamageValue(-1)` fought Unbreaking. Clamp to before-snapshot stacks w/ Unbreaking
- Log double-drop: `level*0.025`. No whole-tree-fell perk — Dynamic Trees already fells the whole
  tree natively from a single broken block

### War path — DONE ✅

**Warrior**
- XP: `MobKillMap` (kill) + `MobDamageMap` (dmg dealt, ~1/5 of kill) + 1 XP/tick taking dmg +
  Armor Stand=0.05 (anti-abuse)
- Mob tiers: S+ (Warden 500/3), S (Wither/Dragon/PiglinBrute/Ravager 300/3),
  A (Evoker/Vex/ElderGuardian/Vindicator/WitherSkeleton/IronGolem 75/1.5),
  B (Ghast/Shulker/Blaze/Hoglin/Zoglin/Enderman/Skeleton/Stray/Drowned/CaveSpider/MagmaCube/
  Breeze/Bogged 15/1), C (Zombie/Husk/Spider/Slime/Phantom/Pillager/Piglin/ZombifiedPiglin/
  Creeper 5/1), D (Silverfish/Endermite/Wolf/Llama/Bee/PolarBear/Goat/Armadillo 1/0.2)
- Access (`ItemUseTierMap`, spec=WARRIOR): L1 leather, L2 chainmail+shield, L3 iron armor+sword
  (bow unrestricted), L4 gold armor, L5 diamond armor+sword+axe, L7 netherite armor+sword+axe +
  mace (trident unrestricted)
- Melee dmg bonus: `+2.5%/lvl`. Dmg taken reduce: `-2.5%/lvl`

**Archer**
- XP: 2 per arrow hit (arrow kill no extra)
- Access: crossbow = craft `ItemCraftTierMap` (Blacksmith 4) + wear `ItemUseTierMap` (Archer 3)
- Bow/crossbow dmg: `0.75 + level*0.05` (L0=75%, L5=100%, L10=125%)
- Armor ignore @L5: `0.10 + (level-5)*0.04` (L5=10%, L10=30%) — via target ARMOR/ARMOR_TOUGHNESS
  attrs (`getDamageAfterArmorAbsorb` protected)
- Double arrow @L5: `0.25 + (level-5)*0.05` (L5=25%, L10=50%), via `ArrowLooseEvent`,
  `ArrowItem.createArrow`
- Faster draw: `+3%/lvl` (max +30%) — `BowItemMixin`, `@Redirect` on `BowItem.getPowerForTime(I)F`
  in `releaseUsing`. Mechanical only — pull animation unchanged (deferred client mixin)

### Magic path — DONE ✅

**Alchemist** — DONE ✅ (simple path)

1. **XP actions** — brew-cycle completion; craft magic consumables (`ItemCraftMap`,
   spec=ALCHEMIST): blaze powder=4, glass bottle=2, reagents (ferm eye/glistering melon/magma
   cream)=3, dyes=0.5; mine natural flowers+glowstone (`BlockXPMap`, path=MAGIC, w/
   `PlacedBlockTracker`): small flowers=0.5, tall=1, glowstone=2. ⚠️ Flowers MOVED from Harvest;
   pink_petals/spore_blossom stay Harvest.
2. **Brew XP map** (`data/BrewXPMap`): XP per potion (`PotionUtils.getPotion(result)`), summed
   over non-empty bottles (1–3). Key = result `MobEffect` (long/strong inherit base). Tiers:
   base (awkward/mundane/thick)=2, T1 (nightvis/swift/leap/slowfall/waterbreath)=5, T2 (fireres/
   heal/poison/weak/invis/slow)=8, T3 (strength/regen/turtle=resistance/harming)=12; modifier
   (long/strong/splash/lingering present) = +2 to base.
3. **Effects:**
   - **Access ladder** (`data/PotionTierMap`) — "DON'T START brew" gate (bottles/reagent stay,
     blaze powder not spent). Req level = max(effect level, long→5, strong→6, splash→7,
     lingering→9). L1 nightvis/swift/leap; L2 waterbreath/slowfall/fireres; L3 heal/poison;
     L4 strength/weak/slow; L5 regen; L6 invis+resistance(turtle); L8 harming.
   - **Active bonus** — reagent economy: chance to not spend reagent. Snapshot (`lastIngredientMap`)
     at gate; on success + roll `level*0.05` (L10=50%) return 1 to slot 3. (Double output /
     duration / amplify REJECTED.)
4. **Formulas:** brew success = `base + (1−base)·ScalingFormula(eff, 1.0, 0.3)`,
   `eff = max(0, alchemistLevel − potionReqLevel)`. Just-unlocked (eff 0) = base (0.4); L1 potion
   @L10 ≈ 84%; harming (L8) @L10 ≈ 63%. **Fail** = only potions destroyed (slots 0–2); reagent
   untouched (`doBrew` already took exactly 1). **XP awarded on success AND fail** (equal).

*Gating/attribution impl (simple path — no slot lock):*
- No single result slot: result computed per bottle (`PotionBrewing.mix(reagent, bottle)`). Brew
  = 400 ticks. No player in `serverTick`/`doBrew` → track owner ourselves.
- `BrewingStandBlockEntity.serverTick` recomputes `isBrewable` EVERY tick.
- **Gate**: `BrewingStandMixin.krp$gateBrewable` — `@Redirect` on `isBrewable(items)` in
  `serverTick`. Vanilla replica = `MagicSystem.isVanillaBrewable` (`BrewingRecipeRegistry.canBrew`),
  then `MagicSystem.canBrewGate`. `brewTime` via `BrewingStandAccessor`.
- **owner**: `interactorMap` (candidate on `RightClickBlock`) → frozen to `activeBrewerMap` at
  brew start (`brewTime==0` + gate passed). While `brewTime>0` gate not re-checked, owner not
  overwritten.
- **Message** dedup by slot-content hash (`warnedHashMap`), no tick spam.
- XP/economy/map cleanup — `onBrewComplete` (after `doBrew`) by `activeBrewerMap`.
- Hoppers / no owner: NOT gated, NO XP.
- ⚠️ Simple path doesn't lock slots (others can swap contents mid-brew, vanilla behavior). Only
  attribution protected. Full player-aware `BrewingStandMenu` path rejected as overkill.

**Enchanter** — DONE ✅

1. **XP actions** (path MAGIC): enchant item on table, enchant book on table (@L3), apply book on
   anvil (@L5), extract enchant on grindstone (half XP). Craft enchanting table (`ItemCraftMap`,
   spec=ENCHANTER, XP=8) — **moved from Engineer, craftable @L0** (profile tool, no
   `ItemCraftTierMap` gate).
2. **XP map** (`data/EnchantXPMap`): XP per enchant = baseXP(rarity)×level, summed over result
   enchants. baseXP: COMMON=3, UNCOMMON=5, RARE=9, VERY_RARE=16. Grindstone — non-curse only
   (`xpNonCurse`), ×0.5.
3. **Effects:**
   - **Access ladder** (`data/EnchantTierMap`). Rarity tier: COMMON=0, UNCOMMON=0, RARE=3,
     VERY_RARE=5; curse OR max-level enchant (maxLevel>1) → 7. Action tier: book-on-table=3,
     anvil=5.
   - **Table pool filter** (`EnchantmentHelperMixin` on `getAvailableEnchantmentResults`): table
     doesn't offer enchants above player tier — cut from candidates BEFORE weighted pick. Kills
     softlock "offered but unclickable". Active only when generation from table: level put in
     `EnchantSystem` ThreadLocal around `getEnchantmentList` (`EnchantmentMenuMixin`); loot/
     villager/fishing → ThreadLocal empty → vanilla untouched. Book @L<3 → empty pool. Base
     lowered to 0.
   - **Table slot count** (`EnchantTierMap.slotCount`): L0–2 → 1 (top/weakest only), L3–4 → 2,
     L5+ → 3. Server refuse click on locked slot (`EnchantmentMenuMixin` HEAD) + client dim/block
     (`client.EnchantmentScreenMixin`).
   - **Active bonuses** (table): enchant boost (+1 level, `level·0.03`, items only), lapis economy
     (`level·0.05`), XP save (`level·0.05`). (Anvil): level discount (refund `cost·level·0.05`),
     gentle enchant (fail-durability-dmg drops with level: L0=20%, −1.5%/lvl to 5% @L10), ignore
     "Too Expensive" (cost≥40) @L8 (`@ModifyConstant` on `createResult`, ordinal 1+2; stack cap
     kept — anti-abuse).
4. **Formulas:** success = `base + (1−base)·ScalingFormula(eff,1.0,0.3)`,
   `eff = max(0, level − reqLevel)`. Table `base=0.35`, anvil `base=0.2` (config
   `enchantTableBaseChance`/`enchantAnvilBaseChance`). L0 COMMON ≈ 35%, L10 COMMON ≈ 84%.
   **Fail:** table — just-applied enchants removed + durability dmg (20%@L0 → 5%@L10; book gone
   whole); anvil — ONLY book destroyed, item stays, levels charged. XP given on fail (action
   allowed); no XP for access-gate.

*Impl:*
- Table (`EnchantmentMenuMixin`): capture owner — `@Redirect` on `Player.getEnchantmentSeed()` in
  ctor (`@Inject` into ctor banned, gotcha #13); ThreadLocal level around `getEnchantmentList`
  (HEAD/RETURN). HEAD `clickMenuButton` — slot/tier gate (via `EnchantmentMenuAccessor` @Invoker
  `getEnchantmentList`); RETURN ordinal=2 (`return true`) — XP + roll + fail/bonuses.
  `broadcastChanges()`.
- Anvil (`AnvilMenuMixin`): access-gate on ENTRY — TAIL `createResult` clears result+cost for
  unavailable book (no dupe); success/fail roll on HEAD `onTake`. Shift-take blocked
  (`ItemCombinerMenuMixin` on `quickMoveStack`) — shift moves result BEFORE `onTake` (see gotchas).
- Grindstone (`GrindstoneMenuMixin`): target = anon result slot `GrindstoneMenu$4` (NOT $3 —
  field initializer takes $1; verified by bytecode). HEAD `onTake`, inputs from
  `player.containerMenu` (slots 0/1).
- **Combining same items on anvil — NOT implemented** (deferred).

### Craft path — DONE ✅ (Carpenter+Blacksmith+Craftsman)

- `ItemCraftMap` covers: Carpenter (wood), Blacksmith (metal — tools+armor+util, progression via
  tempering not chance), Craftsman (natural mats — leather/wool/clay/ceramics).
- ⚠️ Config `CRAFT_CHANCE_*` **removed** — craft fail chance unused.
- **Tier gate = block result PICKUP** (`SlotMixin` on `Slot.mayPickup`, guard
  `instanceof ResultSlot` — covers workbench + 2×2): result VISIBLE but if level-blocked
  (`isCraftBlocked` = `ItemCraftTierMap` OR Cook gate `FoodTierMap`) — can't take AT ALL. All
  vanilla take paths go through `Slot.mayPickup`: click / Q-drop / double-click via `tryRemove`;
  **shift-click — `AbstractContainerMenu.doClick` QUICK_MOVE checks `slot.mayPickup` and exits
  BEFORE `quickMoveStack` if false**; number-swap same. Return `false` → block + chat warn
  (throttled, server only). Tempering (Blacksmith/Craftsman) applied by `CraftingResultMixin`
  (`@Redirect` on `ResultContainer.setItem` in `slotChangedCraftingGrid`).
  - `burnCraft` in `XPSystem.onCraft` — backstop; for gated unreachable (`mayPickup` blocks take →
    `ItemCraftedEvent` doesn't fire).
- Tooltip: "Требует: …" (wear) — always, `RestrictionSystem.onTooltip`. "Крафт требует: …" — when
  craft grid open: workbench (`CraftingMenu`) OR 2×2 (`InventoryMenu`), client
  `client/CraftTooltipClient` (`Dist.CLIENT`; not on server — gotcha #0).
- Craft double output: `ScalingFormula(level, 0.4, 0.5)` — NOBODY on Craft path (`checkCraftBonus`
  skips COOK/CARPENTER/BLACKSMITH/CRAFTSMAN); effectively only magic specs (Alchemist/Enchanter)
  with `ItemCraftMap` entries.

**Carpenter** — DONE ✅
1. **XP actions** (path CRAFT): craft wooden (+"not quite") items (`ItemCraftMap`, spec=CARPENTER).
   Strip-log (RMB axe) = 1 XP path CRAFT too (bark-stripping is woodworking prep, not mining).
   Chop (log break) stays Lumberjack (MINING).
2. **XP by value**: trinkets (planks/sticks/slabs/stairs/buttons/signs/bamboo-blocks/scaffolding)=1,
   basics (doors/trapdoors/fences/tools/decor)=2, util/beds (barrel/stone tools/beds)=3, transport/
   furniture (boats/shelves/beehive)=4, profession stations/campfires=5.
3. **Effects:**
   - **Access ladder** (`ItemCraftTierMap`, spec=CARPENTER, pickup gate `SlotMixin.mayPickup`):
     - **L0** (no gate): planks, stick, bowl, signs, crafting table, chest, wooden tools, smoker (temp).
     - **L1**: buttons, pressure plates, ladder, fishing rod, doors, trapdoors, stone tools, barrel, beds.
     - **L2**: slabs, stairs, fences, gates, hanging signs, bamboo blocks, bark blocks (Wood/Hyphae
       + stripped, all), scaffolding, decor (composter/frames/painting/armor stand/banners).
     - **L3**: boats + chest boats + rafts, bookshelves (+chiseled), beehive, profession stations
       (lectern/loom/fletching/cartography/smithing table), campfires.
   - ⚠️ Smoker still Carpenter L0 — later → Blacksmith.
   - **Wood economy** (`checkCarpenterEconomy`): craft chance `level*0.05` (max 50%@L10) return 1
     wood ingredient (planks/logs/stick/bamboo). Exact sort: grid remainder (`findWoodInGrid`),
     else matched recipe (`findWoodInRecipe` via `RecipeManager.getRecipeFor`). No double output.
   - **Batch** (`checkCarpenterBatch`): crafting base blanks (planks — `ItemTags.PLANKS`, sticks)
     chance `min(0.4, level*0.04)` bonus pack (planks +2, sticks +1).
   - **Passive — build reach** (`refreshBlockReach`): `Attributes.BLOCK_INTERACTION_RANGE`, L5=+1,
     L10=+2 (linear +0.2/lvl from 5). Transient `AttributeModifier` w/ fixed `ResourceLocation`
     (1.21: not UUID), `Operation.ADD_VALUE`; re-applied via `PacketHelper.syncPlayer` +
     `PlayerRespawnEvent`/`PlayerChangedDimensionEvent`.
4. **Formulas:** economy = `min(0.5, level*0.05)`; batch = `min(0.4, level*0.04)`;
   reach = `1.0 + (level−5)*0.2` @level≥5.

**Blacksmith** — DONE ✅
1. **XP actions** (path CRAFT): craft metal on workbench (`ItemCraftMap`, spec=BLACKSMITH); smelt
   metal (`MetalSmeltMap`, XP on furnace take via `FurnaceResultSlotMixin`); netherite upgrade
   (`SmithingMenuMixin`); repair/combine on grindstone (`RepairXPMap` via `GrindstoneMenuMixin`);
   repair item WITH MATERIAL on anvil (`AnvilMenuMixin.krp$blacksmithRepairXP`, `isValidRepairItem`
   — XP `RepairXPMap`).
2. **XP maps**:
   - Craft (`ItemCraftMap`): copper (lightning rod/spyglass=3, brush=2), gold (gear=15,
     clock/light plate=5), iron (gear=20, chainmail=18, util=15, recovery compass=10, minecarts=6),
     diamond (gear=40), netherite (ingot=20, lodestone=15), stations/machines (smoker/smithing/
     furnace/pistons=5), bow+arrows=3, crossbow=10. Copper 1.21 blocks (chiseled/grate/bulb/door/
     trapdoor + cut copper, all oxidation/wax)=2.
   - Smelt (`MetalSmeltMap`, per item×count): iron/gold=2, copper=1, netherite scrap=4. Gated —
     `SmeltTierMap` (#3).
   - Netherite upgrade: 30 XP. Grindstone repair (`RepairXPMap` by material): wood/stone/gold/
     leather=1, iron/chainmail=2, diamond=3, netherite=4. Anvil material repair — same `RepairXPMap`
     (only real repair of damaged item w/ valid material, `isValidRepairItem`).
3. **Effects:**
   - **Access ladder** (`ItemCraftTierMap`, pickup gate): copper L1, gold L2 (+ early util:
     shears/bucket/flint&steel/shield/stonecutter/bow/clock/light plate/smoker/blast furnace),
     iron L3 (+ gear/chainmail/util/instruments/transport/stations/machines/heavy plate/smithing
     table), crossbow L4, diamond L5, netherite ingot+lodestone L7. ⚠️ Netherite GEAR gated
     separately on smithing table (`SmithingMenuMixin`, L7 + XP 30, **no tempering** — diamond
     durability carried as-is), since `ItemCraftedEvent` doesn't fire for smithing table. ⚠️ Stone
     tools — Blacksmith **no gate** (L0, needed for early iron), XP still to Blacksmith.
   - **SMELT ladder** (`SmeltTierMap`, furnace-input gate `CookGatedInputSlot.mayPlace`, key =
     smelt result): copper L1, iron+gold L2 (+ tools/armor→nuggets), netherite scrap L5. Input w/
     unavailable result not placed (covers manual + shift — `moveItemStackTo` respects `mayPlace`).
     Natural mats (sand/cobble/clay) NOT gated by Blacksmith; food = Cook.
   - ⚠️ **Double smelt gate** (input + XP). Input gate misses raw placed by ANOTHER player/hopper.
     So `XPSystem.onMetalSmelted` also checks `isSmeltBlocked`: no XP for smelting above tier (item
     takeable — not abuse; abuse = XP for unavailable action; closes hopper bypass). "No XP for
     access-gate".
   - **Tempering (durability by level)** — main active effect, replaced fail chance
     (`BlacksmithTemperMap`, `applyBlacksmithTempering`). Fresh tier → 50% durability, next tier →
     100%. Tiers (unlock→full): gold 2→3, iron 3→5, diamond 5→7, netherite 7→10. ONLY on craft
     (workbench) — at result assembly (`CraftingResultMixin`, fixes shift-click, gotcha #15), not
     `ItemCraftedEvent`; netherite upgrade doesn't lower durability.
   - **Anvil repair discount** (`AnvilMenuMixin`, "not book" branch): refund
     `cost·min(0.5, level·0.05)` levels on repair/combine (not pure rename).
   - Double output — removed (`checkCraftBonus` skips BLACKSMITH).
4. **Formulas:** temper `quality = clamp(0.5 + 0.5·(level−unlock)/(full−unlock), 0.5, 1.0)`,
   `damage = round((1−quality)·maxDur)` (clamp to `maxDur−1`); anvil `refund = round(cost·min(0.5,
   level·0.05))`.
   - ⚠️ Moved to Blacksmith: smoker (was Carpenter L0), smithing table (was Carpenter L3), minecart
     (was Engineer dup). Leather+horse armor **removed** from Blacksmith map.

**Craftsman** — DONE ✅ (replaced Engineer)
Master of all NON-metal NON-wood: textile, leather, ceramics/clay, **glass, concrete, stone
masonry** (stone/cobble/andesite/diorite/granite/deepslate/sandstone/blackstone/quartz/prismarine/
purpur/end-stone — bricks/slabs/stairs/walls/polish).
1. **XP actions** (path CRAFT): craft natural-mat items (`ItemCraftMap`, spec=CRAFTSMAN) — textile
   (wool from string, carpets, candles), glass (panes/stained/tinted), concrete powder, leather
   (from rabbit hide, leather armor), books/paper, clay/ceramics (clay block, bricks, pot, dyed
   terracotta, decorated pot), all masonry; smelt natural mats (`NaturalSmeltMap` via
   `FurnaceResultSlotMixin` → `XPSystem.onNaturalSmelted`).
2. **XP scales by access tier** (= gate level):
   - Craft (`ItemCraftMap`): L0 base build (textile/candles/glass panes/concrete/basic masonry/
     brick/granite-diorite-andesite polish/misc)=1 (clay=2); L1 leather/book=2, leather boots/helm=3;
     L2 leather chest/legs=3, dyed terracotta=2; L3 decorated pot=4, stained glass=2; L4 chiseled/
     mossy=3; L5 deepslate=4; L6 blackstone/quartz=5; L7 prismarine/purpur/end-stone=6. XP per
     craft-event (slab batch = 1 event). Tuff family 1.21 → base masonry (L0=1), chiseled_tuff → L4.
     Turtle helmet → craft L3.
   - Smelt (`NaturalSmeltMap`, per item×count): glass/stone/smooth+cracked=0.5, brick/nether brick/
     terracotta/glazed=1, sponge=2. **Not gated** (glass/stone needed early) — XP only.
3. **Effects:**
   - **Craft access ladder** (`ItemCraftTierMap`, spec=CRAFTSMAN, pickup gate) — spread 1→7:
     L0 base build; L1 leather tan+book+leather boots/helm+turtle helmet(→L2 gate actually, see
     ItemCraftTierMap); L2 leather chest/legs+dyed terracotta; L3 decorated pot+stained glass; L4
     chiseled/mossy stone; L5 deepslate; L6 blackstone+quartz; L7 prismarine+purpur+end-stone.
     Future modded natural-mat items go here. **Backpacks** (`compat/BackpacksCompat`, soft via
     `ModList.isLoaded`): `backpacks:backpack`=Craftsman 3, `backpacks:large_backpack`=Craftsman 5
     (`ItemCraftTierMap.addById`); config `backpackNesting`=`false`. ⚠️ Leather armor **worn** by
     Warrior (L1, `ItemUseTierMap`) but **crafted** by Craftsman — dual pattern.
   - **Material economy** (`checkCraftsmanEconomy`): chance `min(50%, level·5%)` return 1 natural
     ingredient (leather/string/wool/clay/rabbit hide). Exact sort (grid → recipe).
   - **Batch** (`checkCraftsmanBatch`): base build block (glass_pane, bricks, stone_bricks,
     nether_bricks, mud_bricks, sandstone, red_sandstone, quartz_block, clay) chance
     `min(40%, level·4%)` bonus pack +2.
   - **Tempering** (`applyCraftsmanTempering`, `CraftsmanTemperMap`): fresh leather armor durability
     grows w/ level (50%@tier unlock → 100%). Boots/helm (unlock 1, full 6), chest/legs (unlock 2,
     full 8). ⚠️ Tempering (both Blacksmith+Craftsman) applied at **result ASSEMBLY**
     (`CraftingResultMixin` → `applyTemperingToCraftResult`), NOT `ItemCraftedEvent`: shift-click
     fires event after result moved as separate stack (gotcha #15) → tempering lost.
   - ⚠️ Craft double output REMOVED for Craftsman (`checkCraftBonus` skips CRAFTSMAN).
4. **Formulas:** economy = `min(0.5, level·0.05)`; batch = `min(0.4, level·0.04)`, +2; temper
   `quality = clamp(0.5 + 0.5·(level−unlock)/(full−unlock), 0.5, 1.0)`.
   - ⚠️ `STONE_BRICKS` moved Blacksmith→Craftsman.
   - ⚠️ Known MCP typo: field `Items.CUT_STANDSTONE_SLAB` (not `CUT_SANDSTONE_SLAB`); `Blocks` name
     correct. Red variant `CUT_RED_SANDSTONE_SLAB` — no typo.
   - ⚠️ All ex-Engineer content not moved elsewhere REMOVED: redstone logic (comparator/repeater/
     daylight detector/redstone lamp/trapped chest/lever/tripwire/target/note block/jukebox), rails
     (all), TNT, respawn anchor, conduit, beacon — no XP, no gate (craft as vanilla).

### Harvest path — DONE ✅

- XP: crops (`BlockXPMap`, path=HARVEST), fishing (`FishingXPMap`), food craft (`ItemCraftMap`,
  spec=COOK), cooking in furnace/smoker/campfire (`FoodCookMap`). Food production (craft+cook)
  gated by Cook level (`FoodTierMap`).
- Farmer (DONE ✅):
  - **XP actions**: harvest ripe crop (break), plant (small XP), bonemeal (1 XP, valid target
    only), till soil (0.25 XP — `BlockEvent.BlockToolModificationEvent`, `toolAction==HOE_TILL`,
    `!isSimulated()`), berry RMB (1 XP — ripe sweet berry bush age≥2 / cave vines w/ berries;
    `XPSystem.onRightClickBlock`, main-hand guard)
  - **Access ladder** (`BlockTierMap` harvest + `PlantTierMap` plant, spec=FARMER): L0 wheat+sweet
    berries, L1 carrot+potato+bamboo, L2 beetroot+cane+cactus, L3 pumpkin+melon+mushrooms, L4 cocoa,
    L5 nether wart, L6 glow berries+nether mushrooms, L7 torchflower, L8 pitcher. Harvest XP
    correlates w/ tier (2→12), plant XP 0.5–1.5 (small — anti "plant-break" abuse)
  - **Double drop** ripe crop: linear `level*0.05` (L10=50%)
  - **Hoe durability save**: `min(0.3, level*0.03)`, next-tick rollback w/ clamp (like Lumberjack axe)
  - **"Combine" @L5**: harvest ripe `CropBlock` on RMB + auto-replant (no chance). One seed to
    replant, double drop applies. Base crops only (wheat/carrot/potato/beetroot via `combineSeed`),
    `setBlock(getStateForAge(0))`, XP manual (RMB doesn't trigger `BlockEvent.BreakEvent`)
  - **Boosted bonemeal**: +`level` extra applications over vanilla (`BonemealEvent`, extra
    `performBonemeal`). L10 ≈ instant ripe.
  - **Breeding** (`AnimalTierMap`, gate `EntityInteract`, XP `BabyEntitySpawnEvent`): L0 none.
    L1 chicken+pig, L2 rabbit+wolf+ocelot+cat+fox, L3 cow+sheep+mooshroom (+cow milk bucket, sheep
    shear), L4 goat(+milk)+llama+bee+frog+axolotl+armadillo, L5 horse+donkey+mule+camel, L6 turtle,
    L7 panda, L8 hoglin+strider+sniffer. Breed XP 3→15 by tier, shear=1 XP, milk=0 XP (anti-abuse).
    Gate via `Animal.isFood` on adult (`getAge()==0`). ⚠️ Egg-layers (turtle/frog/sniffer) breed via
    eggs → no `BabyEntitySpawnEvent` XP (gate still works). Shear XP direct in `EntityInteract` on
    `readyForShearing()`
  - **Corals removed** from Harvest entirely
  - ⚠️ **Growing crops excluded from `PlacedBlockTracker`** (`PlantTierMap.isGrowable`) — else own
    farm gave no XP (plant → `EntityPlaceEvent` → marked placed). Only must-grow excluded (can't
    place ripe → no abuse); instant-harvest (cane/bamboo/cactus/mushrooms) stay tracked
- Fisher (DONE ✅):
  - Fishing runs through **Tide** mod (opt integration, `compat/TideCompat` + `compat/TideMixinPlugin`).
    Tide replaces vanilla hook w/ own `TideFishingHook extends Projectile`; effects tied to vanilla
    table/hook **removed** (rod durability, double catch, luck-quality, closed-sky ignore, treasure
    w/o open water). Old `FishingHookMixin` deleted.
  - **Faster bite (Tide)** — `compat/mixin/TideFishingHookMixin` (soft-mixin string target, active
    only if Tide loaded — `kingdomrpcore.tide.mixins.json` + `TideMixinPlugin`). MixinExtras
    `@ModifyExpressionValue` on **read** `GETFIELD lureSpeed` in `TideFishingHook.catchingFish` →
    from Fisher level 5 returns `lureSpeed + 1` (one Lure step), below 5 no bonus. Owner via
    `Projectile.getOwner()` (cast `(Projectile)(Object)this`). ⚠️ Can't `@Redirect` on GETFIELD:
    handler must take EXACT owner type (`TideFishingHook`), unnameable w/o hard dep → boot-crash.
    `@ModifyExpressionValue` takes `(int original)` + `this`.
  - XP map (`XPSystem.onFishing` ← `ItemFishedEvent`) works: Tide posts it read-only. Vanilla
    `FishingXPMap` has priority; Tide fish rarity auto-mapped — `TideCompat` on `AddReloadListenerEvent`
    reads Tide datapack `data/<ns>/fishing/fish/**.json` (`journal_profile.rarity`, `fish`=ID) →
    `volatile Map<Item,Float>`; `FishingXPMap.get` asks `TideCompat.fishXP` after vanilla, before
    `JUNK_XP`. 5 rarities → 5 levels: common→5, uncommon→8, rare→10, very_rare→15, legendary→25.
    Junk stays (`surface_junk`/`underground_junk`/`lava_junk`) → `JUNK_XP=3`.
  - **Tide tackle gate** (`TideCompat`, by ID, soft): craft — `ItemCraftTierMap` (baits 1–7, hooks
    1–8, lines 1–5, angling table 1); rod use (cast) — `ItemUseTierMap` (vanilla 0, stone 1, iron 2,
    gold/crystal 3, diamond 4, echo 5, netherite/prismarine 6, village/blazing 7, honeycomb 8,
    sunflower 9, midas 10). Bobbers not gated.
  - **Tide fish cooking** — Cook gate (`FoodTierMap` + XP `FoodCookMap`): small_cooked_fish/
    cooked_fish_slice=tier1, cooked_fish=2, grilled_tuna=4, large_cooked_fish=5. Key = result.
  - **Tide OP items** (enchanted_pocket_watch / starlight_bow / dragonfin_boots) recipes disabled by
    override in `data/tide/recipe/*` w/ `neoforge:false` (our datapack loads AFTER Tide via optional
    dep `ordering=AFTER`).
  - **Journal**: `fishing_journal` on join disabled in `config/tide.json5` (`journal.giveJournal=false`).
  - Vanilla `FishingXPMap`: junk=3, common=5 (cod/salmon), uncommon=8 (tropical/pufferfish),
    treasure=25 (`isTreasure`: enchanted_book/name_tag/saddle/nautilus_shell/bow/fishing_rod).
    Natural sea flora (`BlockXPMap`, path=HARVEST, `PlacedBlockTracker`):
    kelp/seagrass/tall_seagrass/lily_pad/sea_pickle=1. Sponge → Miner.
- Cook (DONE ✅):
  - **XP actions**: cook in furnace/smoker (XP on result take), campfire (XP on raw place), food
    craft on workbench.
  - **XP maps**:
    - Cook (`FoodCookMap`, key=result): base (dried kelp/baked potato)=1, fish (cooked cod/salmon)=2,
      meat (chicken/rabbit/mutton)=3, fat meat (steak/porkchop)=4. XP per item × count.
    - Craft (`ItemCraftMap`, spec=COOK): cookie/bread=2, soups=4, pumpkin pie=6, rabbit stew=8, cake=12.
  - **Access ladder** (`FoodTierMap`, gates PRODUCTION — craft AND cook, by hunger+saturation value):
    L0 cookie+dried kelp+bread (bread deliberately base), L1 baked potato+cooked cod+cooked rabbit,
    L2 pumpkin pie+soups+cooked chicken, L3 cooked salmon+cooked mutton+cake, L4 steak+porkchop,
    L5 rabbit stew.
  - **Gate mechanism**:
    - Food craft — pickup gate `SlotMixin.mayPickup` (`isCraftBlocked` includes Cook `FoodTierMap`).
    - Campfire — `SpecializationEffects.applyCampfireGate`: cancel RMB place + inventory resync +
      msg. Result/free-slot via `CampfireBlockEntity.getCookableRecipe` (`CookSystem.campfireResult`).
      ⚠️ Campfire gate+XP for **both hands** (via `event.getItemStack()` BEFORE `MAIN_HAND` guard).
    - Furnace/smoker — INPUT gate: raw slot swapped to `CookGatedInputSlot`
      (`AbstractFurnaceMenuMixin`, ctor redirect), `mayPlace` forbids raw w/ unavailable smelt result
      (`CookSystem.smeltResult` → `canProduce`). Covers manual + shift. XP on result take
      (`FurnaceResultSlotMixin`, `checkTakeAchievements`). ⚠️ **XP-gate on take**:
      `CookSystem.onCooked` checks `canProduce` — no XP above level even if raw placed by other
      player/hopper. Same double gate (input+XP) as Blacksmith smelt.
  - **No active bonus effects** — only production access ladder + cook XP. Double food craft removed
    (`checkCraftBonus` skips COOK).
  - Common logic — `system/CookSystem` (helper lib, not subscriber:
    `canProduce`/`sendRestriction`/`campfireResult`/`onCooked`). Respects `KRPConfig.RESTRICTIONS_ENABLED`.

---

## Other infrastructure

- `PlacedBlockTracker` — `Set<Long>` (`BlockPos.asLong()`), in-memory, NOT persistent (resets on
  server restart — deliberate, mirrors original KRP)
- `/krp stats` — all; `/krp addxp|setlevel|reset` — `requires(perm 2)`; `/krp debug` — all
- **Player data — Data Attachment** (`registry/KRPAttachments.PLAYER_DATA`,
  `AttachmentType.serializable(PlayerData::new).copyOnDeath()`); `PlayerData` implements
  `INBTSerializable<CompoundTag>` (methods take `HolderLookup.Provider`). Access `player.getData(...)`
  (never null) / `setData(...)`. `PlayerDataEvents` — client sync on login only.
- **Net** — `network/NetworkHandler` registers in `RegisterPayloadHandlersEvent` via
  `PayloadRegistrar`; packets = `record ... implements CustomPacketPayload` w/ `StreamCodec`
  (`SyncPlayerDataPacket`, `XPGainPacket` — `playToClient`; `ChooseSpecializationPacket` —
  `playToServer`). Send via `PacketDistributor.sendToPlayer/sendToServer`.
- `KRPConfig` (**ModConfigSpec**, SERVER): `baseXP=100, xpCurve=1.5`, `deathPenaltyDuration=6000,
  deathXpMultiplier=0.5`, `brewBaseChance=0.4, enchantTableBaseChance=0.35, enchantAnvilBaseChance=0.2`,
  `restrictionsEnabled=true`. ⚠️ `penaltyCoefficient` + `CRAFT_CHANCE_*` removed.
- `ItemCraftMap` inits **lazily on first `get()`** (config-independent). `ModConfigEvent` binding +
  `reset()` removed.

---

## Game design (invariants)

- **Spec points**: 1 path level = 1 point = 1 spec level. NOT rising cost — tested and rejected
  (caused "level-up button unavailable at N+1").
- **XP penalty by path level AT AWARD TIME**, not by invested spec points — else abuse (hoard levels
  without investing, then dump).
- **Path max level = #specs × 10**, not fixed — recomputes after spec roster changes.
- **Craft: fail = resources burn** (not returned) — else infinite free retries.
- **XP awarded EVEN ON chance-fail** (brew, table enchant, anvil-book, chance craft) — same as
  success. Else spec stalls at low chance. Punishment = lost RESOURCE/item, not XP. ⚠️ ONLY for
  **chance-fail of an ALLOWED action**. For **access-gate** (no spec level at all —
  `ItemCraftTierMap`/`FoodTierMap`/`PotionTierMap` "don't start") NO XP.
- **Enchant: fail = item destroyed**, not "enchants didn't stick" — else no risk (Mending trivializes).
- **Natural blocks**: from original KRP (`SyntheticBlockHolder.kt`) — simple in-memory map, no
  persistence. Deliberate.
- **Farmer vs PlacedBlockTracker**: seed plant via `EntityPlaceEvent` → cell marked placed → own
  farm gave no XP. Fix: growing crops (`PlantTierMap.isGrowable`) not tracked. No abuse (can't place
  ripe). Instant-harvest (cane/bamboo/cactus/mushrooms) tracked normally.

## History & gotchas

- Decision/fix chronology — `docs/DECISIONS_LOG.md`.
- Low-level technical gotchas (Mixin / slots / events / screens) — `docs/TECH_GOTCHAS.md`.

---

## Current state

**Platform: NeoForge 1.21.1**, Java 21, ModDevGradle. 1.20.1 migration done, merged to `main`.

**Done** (4-point format): all 5 paths + specs — Mining (Miner+Lumberjack), War (Warrior+Archer),
Harvest (Farmer+Fisher+Cook), Magic (Alchemist+Enchanter), Craft (Carpenter+Blacksmith+Craftsman).

**Integrations** (soft): Farmer's Delight, Tide (fishing — Fisher runs on it), Backpacks, Dynamic
Trees (Lumberjack XP by felled-tree volume — `DTBranchBlockMixin` on `destroyBranchFromNode`; DT
saplings excluded from `PlacedBlockTracker` so plant→grow→chop stays legit XP, same rule as
Farmer's growable crops; Lumberjack drop/durability/speed perks also trigger on DT branch blocks
via `isDtBranch()`). 1.21 content added to mappings (coverage audit).

⚠️ **Open**: runClient retest of Tide + Dynamic Trees integrations (mixin apply, tackle/rod gates,
fish cooking, journal off; DT volume XP, trunk_shell, no double count, sapling-plant-exemption,
isDtBranch effects). Backlog: stats/logging, guilds/territories — after balance.
