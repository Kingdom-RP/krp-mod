# Kingdom RP — Claude instructions

Minecraft **1.21.1 + NeoForge** mod, RPG specialization system (Kingdom RP). Full
architecture: `docs/KINGDOM_RP_ARCHITECTURE.md` — read at project start. Decision
history: `docs/DECISIONS_LOG.md`. Technical gotchas: `docs/TECH_GOTCHAS.md`.

## Core rules

1. **Warn about game-design changes upfront.** If a change looks like a bug but is
   planned behavior / balance tweak — ask/warn first, don't silently "fix".

2. **Spec format.** Any spec reworks follow this schema:
   - 1. XP-earning actions (path-level, not spec — XP goes to the whole path)
   - 2. XP mapping (block/mob/item → XP table)
   - 3. Spec effects (level-gated content access + active effects)
   - 4. Chances/formulas for the effects in #3

3. **Use enum `Path` / `Spec`**, not magic numbers/strings. `Path.MINING.index`,
   `Spec.MINER.id`, etc.

4. **Mappings = separate classes in `data/`**, not hardcoded if/else in logic. Each
   new data type (BlockEntry, CraftEntry, KillEntry, TierRequirement, ...) = own record.

5. **One handler per event.** Multiple checks on one event (e.g. `BlockEvent.BreakEvent`)
   = ONE `@SubscribeEvent` method (`net.neoforged.bus.api.SubscribeEvent`) calling private
   submethods (`checkMiner`, `checkFarmer`, ...), not several `@SubscribeEvent` on one event.

6. **Natural blocks.** Any XP/effect-from-block-break mechanic must check
   `PlacedBlockTracker.isPlacedByPlayer(pos)` — no bonuses for player-placed blocks.

7. **Send code as text** for small changes (~<150 lines), not as a file. File only for
   many-file edits or large volume.

8. **After any significant gameplay/architecture change** — update
   `KINGDOM_RP_ARCHITECTURE.md` ("Current state" and, if needed, decision/gotcha docs).
   Do it yourself, unprompted.

9. **Short comments.** Terse, to the point — no 5-line comment per 1 line of code. Comment
   only **current state**, not history ("used to be X", "after migration", "gotcha #N",
   "changed because...") — that belongs in `.md` files, NOT in code. Code = what/why the
   line does now, no edit chronology.

## Environment

- Windows, IntelliJ IDEA, **Java 21**, **NeoForge 1.21.1 (21.1.233)**.
- Build: **ModDevGradle** (`net.neoforged.moddev` 2.0.141) + Gradle 8.14. Dep:
  `net.neoforged:neoforge:21.1.233` (via `neoForge { version = ... }`). No reobf (runtime
  on Mojmap / official mappings). Build/check: `./gradlew build`, run: `./gradlew runClient`
  (working dir = `run/`).
- **Mod metadata = `src/main/resources/META-INF/neoforge.mods.toml`** (NOT `mods.toml`).
  1.21 format: `type="required"`, dep on modid `neoforge`.
- **Player data = Data Attachment** (`registry/KRPAttachments.PLAYER_DATA`, `copyOnDeath`),
  access `player.getData(...)` / `setData(...)`; no more capability. **Network** =
  `CustomPacketPayload` + `StreamCodec` + `PayloadRegistrar` (`network/NetworkHandler` on
  `RegisterPayloadHandlersEvent`). **Config** = `ModConfigSpec`. **HUD** = `LayeredDraw.Layer`
  via `RegisterGuiLayersEvent`. **Magic** = Data Components (`PotionContents`,
  `ItemEnchantments`, `Holder<Enchantment>`/`Holder<MobEffect>`).
- Mixin — no MixinGradle; no refmap (Mojmap), `compatibilityLevel JAVA_21`, all mixins
  `remap = false`.
  - **`kingdomrpcore.mixins.json` loaded NATIVELY via `[[mixins]]` `config="..."` in
    `neoforge.mods.toml`** (supported on 1.21; the 1.20.1 `MixinConfigs` manifest +
    `MANIFEST.MF` hack is REMOVED).
  - Verify apply — `run/logs/debug.log`: `Registering mixin config: ...` and
    `Mixing <X> ... into <target>`.
  - ⚠️ **Inject targets checked at RUNTIME, not by compiler.** If a vanilla method
    signature changes between versions, the mixin fails with
    `InvalidInjectionException`/`InvalidAccessorException` only on TARGET-CLASS LOAD (some
    classes load lazily — e.g. `EnchantmentHelper` on first enchant). So after mixin edits
    `build` is not enough — need `runClient` + open the relevant menu.
- Server config: `saves/<world>/serverconfig/kingdomrpcore-server.toml`
