# Technical gotchas (do NOT repeat)

Low-level NeoForge/Mixin traps already hit. Linked from `KINGDOM_RP_ARCHITECTURE.md`.

## Milk-immune effect (curative-items)

- **Milk in Forge/NeoForge cures via `LivingEntity.curePotionEffects(stack)`, not
  `removeAllEffects()`.** Each `MobEffectInstance` holds a curative-items list
  (`getCurativeItems()`, default = milk bucket). `curePotionEffects` removes only effects where
  `isCurativeItem(stack)` true. To make an effect NOT milk-removable (milk still cures vanilla),
  clear the list on apply: `inst.getCurativeItems().clear()`. Empty list persists in effect NBT →
  milk-immunity survives relog.
  - ⚠️ In 1.21 `getCurativeItems()` REMOVED. Protection moved to `EffectCure`:
    `XPSystem.onEffectRemove` (`MobEffectEvent.Remove`) cancels `DEATH_XP_PENALTY` removal on
    `EffectCures.MILK`.

## Slots, machines, shift-click

13. **For MACHINES (furnace/block-entity) gate the slot on INPUT (`mayPlace`), not output.**
    Output gate on a machine is doubly bad: (a) **softlock** — product ready, nobody can take,
    machine jams; (b) **break bypass** — breaking the block drops contents past the gate. Right:
    gate **raw entering the slot** via `Slot.mayPlace(ItemStack)` — vanilla `moveItemStackTo`
    respects it, so ONE point covers manual place + shift-click.
    - ⚠️ **`Slot.mayPickup` covers ALL take paths incl shift-click.** `AbstractContainerMenu.doClick`
      QUICK_MOVE checks `slot.mayPickup` and exits BEFORE `quickMoveStack`. Click/Q/double-click go
      via `tryRemove`, also calling `mayPickup`. So for a CRAFT RESULT slot (not a machine — no
      softlock/bypass) `mayPickup` alone suffices (`SlotMixin`).
    - Cook impl: furnace input slot swapped to `CookGatedInputSlot` via redirect `new Slot` in
      `AbstractFurnaceMenu` ctor. Getting the player is hard: **`@Inject` into ctor BANNED** by Mixin
      ("Found @Inject targetting a constructor"). Fix — `@Redirect` on READ of `Inventory.player`
      (line `this.level = inventory.player.level()` before slots created), cache player, second
      `@Redirect` on `new Slot` uses cache.
    - RMB-action gating (campfire etc.) must cover BOTH hands: guard `event.getHand() != MAIN_HAND`
      skips off-hand — compute gate/XP BEFORE the guard via `event.getItemStack()`.

15. **`quickMoveStack` (shift-click) moves result to inventory BEFORE `Slot.onTake`.** Any
    "decide success/fail and on fail reclaim result" logic in `onTake` is too late for shift. Anvil
    fix: access-gate on ENTRY (`createResult` makes no result), shift-take blocked by separate mixin
    on `quickMoveStack` (`ItemCombinerMenuMixin`).

15b. **MODIFYING craft result (durability tempering) on `ItemCraftedEvent` is lost on shift-click**
    — shift `quickMoveStack` moves result as separate stack (via `split`) BEFORE `ItemCraftedEvent`.
    Fix: modify result stack at **ASSEMBLY** — `@Redirect` on `ResultContainer.setItem` in
    `CraftingMenu.slotChangedCraftingGrid` (`CraftingResultMixin`). Effects that ADD items
    (economy/batch) work on `ItemCraftedEvent` incl shift — bug only for those that CHANGE the
    result stack itself.

14. **Minecraft anon classes numbered by DECLARATION order in source, incl field initializers.**
    Grindstone result slot = NOT `GrindstoneMenu$3` but `$4`: field
    `repairSlots = new SimpleContainer(2){...}` takes `$1`. Check by bytecode (`javap -p`), don't guess.

12. **Canceling `PlayerInteractEvent.EntityInteract`/`RightClickItem` on server without resync =
    client phantom.** Client predicts interaction (bucket→milk, RMB-equip armor) before server
    reply. After `event.setCanceled(true)` call `player.containerMenu.sendAllDataToRemote()` — force
    inventory resync. (LMB armor-equip fixes itself via `onEquipmentChange`.)

## Mixin / events

0. **Client `@EventBusSubscriber` without `value = Dist.CLIENT`** crashes on dedicated server
   (`Attempted to load class .../KeyMapping for invalid dist DEDICATED_SERVER`). Any `client/` class
   with a MOD-bus subscriber must have `value = Dist.CLIENT`. Invisible on client — surfaces on
   `runServer`.

1. **`event.getX() instanceof Y y`** where `getX()` already returns `Y` — compiler complains
   "subtype of pattern type". Fix: `Y y = event.getX();`.

4. **`@Shadow` doesn't work for parent-class fields** (`inputSlots` in `AnvilMenu`, declared in
   `ItemCombinerMenu`). Fix — separate `@Mixin` interface with `@Accessor` on the parent class.

5. **`getDamageAfterArmorAbsorb`** in `LivingEntity` — `protected static`, can't call directly.
   Compute armor-ignore via target `Attributes.ARMOR` / `Attributes.ARMOR_TOUGHNESS`.

6. **`@At("TAIL")` inject in a method with several `return`s** fires only on the LAST. For a specific
   branch — `@At("RETURN", ordinal=N)`, counting `return`s in decompiled bytecode.

7. **Config-dependent mappings** can't init in a static block if they use `ModConfigSpec` values —
   config not loaded at class-loading. Fix: lazy `init()` (+ recompute on `ModConfigEvent`).

8. **Bonus drop / fellTree via `playerDestroy` inside `BlockEvent.BreakEvent`** can recurse events —
   run via `server.execute()` (next tick).

10. **`@Redirect` on field READ inside `field -= expr`** can't change the field: JVM loads the old
    value onto stack BEFORE `expr`, then writes `old − result`, clobbering the handler's write. Fix —
    intercept the **write** (`opcode=PUTFIELD`). Cleaner alt — MixinExtras `@ModifyExpressionValue`
    on read: takes `(int original)`, instance via `this` (e.g. `TideFishingHookMixin`).

11. **`@Shadow` of a parent-class field with `remap=false` no refMap not found** (`@Shadow field X
    was not located`). E.g. `random` declared in `Entity`, mixin on `FishingHook`. Fixes: (a) don't
    shadow, get value otherwise; (b) `@Accessor` interface on parent (like #4). Target-class own
    fields shadow fine.

## Screens / client

9. **Dynamic button lists (PathScreen/SpecializationScreen)** — compute window height + Y positions
   BEFORE render (in `init()`/helper), reuse in `render()`.

- **Blurred screens in 1.21.** `Screen.render` ITSELF calls `renderBackground` →
  `renderBlurredBackground`. Override `renderBlurredBackground` + `renderMenuBackground` empty, do own
  `graphics.fill(...)` dim. Override the submethods, not `render` itself.

- **Main-menu customization.** `TitleScreen` loads immediately (mixin NOT lazy). Removing
  Realms/copyright — via `ScreenEvent.Init.Post` + `event.removeListener(...)`, **NOT
  `@Shadow Screen.removeWidget`** (shadow of inherited method didn't attach: `InvalidMixinException`).

## Build

2. **Gradle version + build plugins** — don't blindly accept IntelliJ's "update Gradle": ModDevGradle/
   toolchain pinned to specific versions.
