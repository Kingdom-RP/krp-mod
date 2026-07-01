# Decision log (chronology)

Dated notes on reworks + fixes. Current system state — `KINGDOM_RP_ARCHITECTURE.md`; technical
gotchas — `TECH_GOTCHAS.md`.

## Dynamic Trees: Lumberjack XP (2026-06-30)

DT replaces trees with its own `BranchBlock` — not in `BlockXPMap` (vanilla `Blocks.*_LOG`), so
chopping DT gave no XP. DT worldgen default **replaces** vanilla trees (`worldGen=true` +
FeatureCanceller), i.e. no natural vanilla trees; vanilla log-XP kept (placed logs / non-DT species
/ worldGen off).

**Final — soft-mixin `compat/mixin/DTBranchBlockMixin`** (gate `DTMixinPlugin`,
`kingdomrpcore.dynamictrees.mixins.json`). Hook — `BranchBlock.destroyBranchFromNode` (@Inject
RETURN): called ONCE per actual fell, returns `BranchDestructionData` with real `woodVolume` (in
logs). XP = `min(40, logs·2)` (parity with `oak_log`=2). DT types via reflection (not on
compile-classpath), volume pre-Fortune. `entity` = player (vanilla type).

Why not BlockEvent+tag/radius (early attempts, dropped):
- **trunk_shell** (thick trunk) not in `branches` tag → break felled tree with no XP; via
  `destroyBranchFromNode` (shell fells core branch) — caught.
- **double count**: radius-per-BreakEvent gave 25 for both upper branch and remainder finish.
  `woodVolume` counts from cut upward → finishing gives ONLY remainder, no re-reward.
⚠️ Other Lumberjack perks (speed/axe/double-drop/fell) don't apply to DT — own fell mechanism.

## Tide + Backpacks + localization (2026-06-28)

- **Fishing moved to Tide** (opt integration `compat/TideCompat` + `TideMixinPlugin`). Tide replaces
  vanilla hook — vanilla-hook effects removed (double catch, durability, luck, sky/water) +
  `FishingHookMixin`.
- Faster bite under Tide: `TideFishingHookMixin` (`@ModifyExpressionValue` on read `lureSpeed` in
  `catchingFish`), +1 from Fisher level 5.
- Catch XP by species rarity (read Tide datapack `fishing/fish` on reload): common 5 / uncommon 8 /
  rare 10 / very_rare 15 / legendary 25.
- Tackle gate by Fisher level (craft baits/hooks/lines/rods + rod use); Tide fish cooking under Cook.
  OP items disabled by recipe override (`neoforge:false`, dep `ordering=AFTER`). Journal-on-join off
  in Tide config.
- **Backpacks**: backpack craft = Craftsman 3, large = 5; item translations. Paths-menu keybind
  localization. Fix craft tooltip in 2×2 (`InventoryMenu`).

## 1.20.5–1.21.1 content in mappings + audit (2026-06-28)

Mappings built for 1.20; new content added by existing-map logic:
- **Tuff family** → Craftsman (masonry L0, `chiseled` L4).
- **Copper 1.21** (chiseled/grate/bulb/door/trapdoor + cut copper, all oxidation/wax) → Blacksmith,
  copper tier 1.
- **Mace** → Warrior wear L7. **Turtle helmet** → Craftsman craft tier 2, Warrior wear L2.
- **Mobs**: breeze/bogged → Warrior tier B; armadillo → Harvest + Farmer breed L4.
- ⚠️ **Compression blocks (9→1) NOT mapped** — craft gives XP → uncraft back → abuse. Cut copper
  doesn't break this (1:1).
- Audited all 1255 items (diff `Items.java` vs `Items.X`+`Blocks.X`). Skipped: concrete (powder-in-
  water, no craft event), corals (removed from Harvest), redstone/rails/beacon/conduit (policy),
  seeds/saplings/leaves (gate by block), raw drops (XP for ore/mob), crafter/vault/heavy core (no
  craft/loot).

## Lumberjack: XP on gate + biome lockout (2026-06-20)

- **Bug: XP for gated mining.** Tier gate (`checkTierRestriction`) and XP (`XPSystem.onBlockBreak`)
  — DIFFERENT `@SubscribeEvent`. Fix: gate given `EventPriority.HIGH`, `XPSystem.onBlockBreak` starts
  with `if (event.isCanceled()) return;`. ⚠️ Two handlers on one event in different classes — order
  not guaranteed.
- **Balance: overworld trees ungated** (tier-0 in `BlockTierMap`), else savanna/swamp player can't
  get local wood. Gate only nether stems / giant mushrooms (L6).

## Future: stats + logging (backlog, 2026-06-20)

Not doing yet. Progress storage — Data Attachment → NBT in `playerdata/`. Analytics = additive layer.
- **Progress logging**: append-only event-log (CSV/JSON) at level-up points.
- **Online stats** — iterate `server.getPlayerList().getPlayers()`.
- **All-players stats (incl offline)** — secondary index (SQLite/H2 "stats.db"), updated on
  logout/level-up.
- Principle: Data Attachment = source of truth; log/DB = derived.

## Farmer's Delight integration (soft, 2026-06-21)

Cross-mod compat pattern WITHOUT hard dep:
- **Maps by ID** — `addById(String id, …)` (`BuiltInRegistries.*.getOptional`, no-op if absent).
- **`compat/FarmersDelightCompat`** (`FMLCommonSetupEvent`, `enqueueWork`, gate by `ModList.isLoaded`):
  crops (Farmer), food (Cook, tiers 1–10 by value), knives (Blacksmith).
- **Cooking Pot gate + XP** (`compat/mixin/CookingPotResultSlotMixin`, string target, `remap=false`):
  gate in `remove(int)` → `ItemStack.EMPTY` if under-level; XP in `onTake` via `CookSystem.onCooked`.
  ⚠️ `CookingPotMealSlot` = preview slot; real take = `CookingPotResultSlot`. Separate config
  `*.farmersdelight.mixins.json` + `FDMixinPlugin`.
- **Generalized immature guard** (`XPSystem.isImmatureCrop`): vanilla `CropBlock` → `isMaxAge`,
  modded → by `age` property.
- ⚠️ NOT covered: Cutting Board (knife cut — separate mechanic).

## Farmer: removed plant XP — exploit (2026-06-21)

XP leaked from **planting** (`WorldEvents.onBlockPlace`): seeds returned on sapling break →
plant→break→replant = infinite farm. Fix: plant XP removed (plant gate + cell tracking kept).
Compensation: crop HARVEST-XP in `BlockXPMap` +1. `PlantEntry.plantXP` kept in data, not awarded.

## Hunger balance (Cook/Mining relevance, 2026-06-20)

- **Respawn at 50% hunger** (`XPSystem.onPlayerRespawn`, config `balance.respawnFoodLevel`=10),
  skipped on `isEndConquered`.
- **Hunger drain ×2** (`FoodDataMixin` → `@ModifyVariable` on `FoodData.addExhaustion`,
  `balance.exhaustionMultiplier`=2.0). ⚠️ SERVER config — before sync client `SPEC.isLoaded()` may be
  false (crash guard).
- **TODO**: reduce hunger restore from food (mixin `FoodData.eat`/per-item).

## Anti-grief for release (2026-06-20)

- **Hard craft ban** (`BannedCraftMap` + `RestrictionSystem.isCraftBanned`,
  `antiGrief.craftBanEnabled`): TNT, TNT minecart, End crystal, hopper(+minecart), observer, pistons,
  dispenser, dropper. Checked BEFORE spec-gate; block via `SlotMixin.mayPickup`.
- **End closed** (`onTravelToDimension`, `EntityTravelToDimensionEvent`, `antiGrief.closeEnd`): cancel
  teleport when `getDimension() == Level.END`.

## /krp commands: path by name + target player (2026-06-20)

- Path arg — **name** (`craft`/`harvest`/…) w/ autocomplete; index 0–4 also accepted
  (`KRPCommand.parsePath`).
- `addxp`/`setlevel`/`reset`/`stats`/`debug` — opt `target` (`EntityArgument.player`); mutating cmds
  + non-self target need `permission(2)`. Output via `sendSuccess`.

## Main-menu customization (2026-06-20) — ⚠️ REMOVED 2026-06-29 (see FancyMenu section below)

`client.TitleScreenMixin` (`@Mixin(TitleScreen)`, remap=false):
- `@Redirect BrandingControl.forEachLine` → empty (hide version lines).
- `@Redirect SplashRenderer.render` → empty (splash hidden).
- `@Redirect LogoRenderer.renderLogo` → blit `title_logo.png` (512×160), centered, width 380. Image,
  not font.

`ClientEvents.onTitleScreenInit` (`ScreenEvent.Init.Post`): removes Realms/copyright (scan
`getListenersList()`), shifts widgets up 24 px (see TECH_GOTCHAS).

## Server-side client mod check (2026-06-20)

Anti-cheat whitelist on NeoForge 1.21 handshake (config phase):
- `ModWhitelistConfigurationTask` (`ICustomConfigurationTask`) per connection
  (`RegisterConfigurationTasksEvent`) → sends `ModCheckRequestPayload`.
- Client replies `ModListReplyPayload` (its `ModList.get().getMods()`).
- Server compares with **server mods ∪ `extraAllowedMods`**; extras → `disconnect`.
- Payloads **required** (not optional) → vanilla/non-Neo clients cut by handshake. Config (SERVER):
  `modCheck.enabled`, `modCheck.extraAllowedMods`.
- ⚠️ Client reports its own list — guards honest/casual, not deep-modified. `finishCurrentTask` throws
  outside config phase; config payloads = `FriendlyByteBuf` codecs.

## Post-release 1.21 migration retest — fixes (2026-06-19)

- **Blurred screens** — override `renderBlurredBackground`/`renderMenuBackground` empty (see TECH_GOTCHAS).
- **Milk removed XP penalty** — `EffectCure` via `XPSystem.onEffectRemove` (`getCurativeItems` gone in 1.21).
- **Farmer leveled on immature** — `onBlockBreak` skips `CropBlock && !isMaxAge`.
- **Anvil lied about book req level** — outputs real `krp$requiredFor`; removed double message
  (`createResult` on client AND server menu — send only `!isClientSide`).
- **Bow**: `BowItemMixin` removed (broken, dup `ArrowLooseEvent`). New Archer perk — **arrow range**
  (`onArrowSpawn`, speed ×(0.5 + 0.1·lvl)). Pull animation unchanged.
- **Tempering extended**: wood → Craftsman (`CraftsmanTemperMap` 1/3), stone → Blacksmith
  (`BlacksmithTemperMap` 1/4).

## Icons / menu / window title (2026-06-29: removed for FancyMenu)

- **Mod logo**: `logoFile="logo.png"` in `neoforge.mods.toml` (128×128 PNG) — kept (mod-list icon,
  not window).
- **Main-menu customization removed**: `TitleScreenMixin` (branding/splash hide, logo swap), Realms
  removal + widget shift — all moved to **FancyMenu**. Kept only **copyright hide**
  (`ClientEvents.onTitleScreenInit`, widget `title.credits`).
- **Custom window icon + title removed** (`WindowIcon`, `ClientModEvents`, `MinecraftTitleMixin`) —
  set via mod. Assets `icons/`, `gui/title_logo.png` deleted.
