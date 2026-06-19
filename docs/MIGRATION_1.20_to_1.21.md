# Миграция Kingdom RP: NeoForge 1.20.1 → 1.21.1

Документ описывает переезд мода с NeoForge 1.20.1 на 1.21.1: что и зачем менялось,
известные изменения поведения/баланса, потенциальные ошибки и **план полного
ретеста**, чтобы убедиться, что миграция ничего не сломала.

---

## 1. Зачем переезжали

- Для **NeoForge-мода** 1.21.1 даёт ~16.6k модов на Modrinth против ~4.6k на 1.20.1
  (на 1.20.1 NeoForge — нишевый форк, основная масса под LexForge). 1.21.x — текущий
  растущий хаб моддинга.
- 1.20.1 был ранним форком NeoForge с пакетами `net.minecraftforge.*`; 1.21.1 — это
  «настоящий» NeoForge со всеми API-изменениями (пакеты, capabilities, сеть,
  Data Components).
- Промежуточные версии (1.20.2–1.20.6) — «провал» по числу модов, поэтому прыгнули
  сразу на 1.21.1.

## 2. Тулчейн и метаданные

| | Было (1.20.1) | Стало (1.21.1) |
|---|---|---|
| MC / NeoForge | 1.20.1 / 47.1.106 (`net.neoforged:forge`) | 1.21.1 / 21.1.233 (`net.neoforged:neoforge`) |
| Java | 17 | **21** |
| Gradle-плагин | NeoGradle 7.0.192 | **ModDevGradle 2.0.141** (`net.neoforged.moddev`) |
| Метаданные | `META-INF/mods.toml`, dep `forge` | **`META-INF/neoforge.mods.toml`**, `type="required"`, dep `neoforge` |
| Mixin-конфиг | атрибут манифеста `MixinConfigs` (+ ресурсный `MANIFEST.MF`) | **нативно `[[mixins]]`** в `neoforge.mods.toml` |

Удалено за ненадобностью: ресурсный `META-INF/MANIFEST.MF`, `MixinConfigs`/
`exclude 'META-INF/MANIFEST.MF'` из jar-задачи.

## 3. Карта изменений API (что переписано)

| Область | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
|---|---|---|
| Пакеты | `net.minecraftforge.*` | `net.neoforged.neoforge.*` / `net.neoforged.fml.*` / `net.neoforged.bus.api.*` |
| Подписчик | `@Mod.EventBusSubscriber` | `@EventBusSubscriber` (`net.neoforged.fml.common`) |
| Главный класс | `@Mod` + `FMLJavaModLoadingContext` | конструктор `@Mod(IEventBus modBus, ModContainer)` |
| Конфиг | `ForgeConfigSpec` | `ModConfigSpec` (API почти 1:1) |
| Реестры | `ForgeRegistries.X`, `RegistryObject` | `BuiltInRegistries`/`Registries`, `DeferredHolder`/`Holder` |
| Данные игрока | Capability + `LazyOptional` | **Data Attachment** (`AttachmentType`, `copyOnDeath`), `getData/setData` |
| Сеть | `SimpleChannel` / `NetworkRegistry` | `CustomPacketPayload` + `StreamCodec` + `PayloadRegistrar` (`RegisterPayloadHandlersEvent`) |
| HUD-оверлей | `IGuiOverlay` / `RegisterGuiOverlaysEvent` | `LayeredDraw.Layer` / `RegisterGuiLayersEvent` (`g.guiWidth()`/`guiHeight()`) |
| Урон | `LivingHurtEvent` | `LivingIncomingDamageEvent` |
| Тики | `TickEvent.ClientTickEvent` | `ClientTickEvent.Post` (фаза END подразумевается) |
| Bonemeal | `BonemealEvent.getEntity()/getBlock()` | `getPlayer()/getState()` |
| Tool actions | `ToolActions.HOE_TILL` | `ItemAbilities.HOE_TILL`, `event.getItemAbility()` |
| Атрибут-модиф. | `AttributeModifier(UUID, name, amount, ADDITION)` | `AttributeModifier(ResourceLocation, amount, ADD_VALUE)`; `getModifier/removeModifier(ResourceLocation)` |
| Дальность блоков | `ForgeMod.BLOCK_REACH` | ванильный `Attributes.BLOCK_INTERACTION_RANGE` (`Holder<Attribute>`) |
| Рецепты | `getRecipeFor(type, Container, level)` | `getRecipeFor(type, RecipeInput, level)` → `Optional<RecipeHolder<T>>`; `CraftingInput.of(w,h,items)`, `SingleRecipeInput(stack)`; `recipe.value()...` |
| `isValidBonemealTarget` | `(level, pos, state, isClient)` | `(level, pos, state)` (boolean убран) |
| `ArrowItem.createArrow` | `(level, ammo, shooter)` | `(level, ammo, shooter, weapon)` |
| Зелья | `PotionUtils.getPotion()`, `Potion.getEffects()` | `stack.get(DataComponents.POTION_CONTENTS)` → `PotionContents.getAllEffects()`; `potion()` = `Optional<Holder<Potion>>` |
| Зачары | `EnchantmentHelper.getEnchantments()` → `Map<Enchantment,Int>` | `getEnchantmentsForCrafting()` → `ItemEnchantments` (ключи `Holder<Enchantment>`); мутация — `updateEnchantments(stack, Consumer<Mutable>)` / `setEnchantments(stack, ItemEnchantments)` |
| Редкость чары | `Enchantment.getRarity()` (enum) | **УБРАНА** → `enchantment.getWeight()` (int 1–1024) |
| Проклятие | `Enchantment.isCurse()` | тег `EnchantmentTags.CURSE` (`holder.is(...)`) |
| Эффекты | `MobEffects.X` = `MobEffect` | `Holder<MobEffect>`; `MobEffectInstance.getEffect()` = `Holder<MobEffect>` |
| Варка | Forge `BrewingRecipeRegistry.canBrew(...)`, `PotionBrewing.mix` (static) | `level.potionBrewing()` (инстанс): `isIngredient`/`hasMix`/`mix`; `isBrewable(PotionBrewing, items)` |
| Screen | `renderBackground(graphics)` | `renderBackground(graphics, mouseX, mouseY, partialTick)` |
| Curative items | `MobEffectInstance.getCurativeItems()` | **УДАЛЕНО** |

## 4. Изменения поведения / баланса (флажки)

1. **XP Зачарователя — теперь по ВЕСУ чары, а не по редкости.** `Enchantment.Rarity`
   убрана. Пороги веса подобраны так, чтобы воспроизвести старые бакеты для
   ванильных чар (вес 10=COMMON, 5=UNCOMMON, 2=RARE, 1=VERY_RARE), поэтому баланс
   ванили сохранён, а модовые чары покрываются автоматически. См. `EnchantXPMap`,
   `EnchantTierMap`.
2. ~~**Молоко снимает дебафф опыта**~~ **ИСПРАВЛЕНО (2026-06-19).** В 1.21
   `getCurativeItems()` убран, лечение идёт через `EffectCure`. Защита возвращена:
   `XPSystem.onEffectRemove` (`MobEffectEvent.Remove`) отменяет удаление
   `DEATH_XP_PENALTY`, если причина — `EffectCures.MILK`. Молоко снова НЕ снимает штраф.
3. **Прогресс игроков сбрасывается.** Хранилище сменилось capability → Data
   Attachment (другой ключ/обёртка), старые сейвы не читаются. Для пре-релиза ОК.
4. **`/krp reset`** теперь зовёт `data.deserializeNBT(player.registryAccess(), new CompoundTag())`
   (сериализация требует `HolderLookup.Provider`).

## 5. Потенциальные ошибки и на что смотреть

- ⚠️ **Цели инжекта миксинов проверяются в РАНТАЙМЕ, не компилятором.** `build`
  зелёный ≠ миксины применятся. При смене сигнатуры ванильного метода миксин падает
  с `InvalidInjectionException`/`InvalidAccessorException` при ЗАГРУЗКЕ целевого
  класса. Часть классов грузится **лениво** (напр. `EnchantmentHelper` — при первом
  зачаровании), поэтому ошибка может вылезти не на старте, а при заходе в меню.
  Проверка применения — `run/logs/debug.log`: `Mixing <X> into <target>` без
  `FATAL/Invalid`. **Уже исправленные в ходе миграции сдвиги сигнатур 1.21:**
  - `EnchantmentMenu.getEnchantmentList` получил 1-й параметр `RegistryAccess`
    (`EnchantmentMenuAccessor` @Invoker + 2 @Inject в `EnchantmentMenuMixin`);
  - `CraftingMenu.slotChangedCraftingGrid` получил 6-й параметр
    `@Nullable RecipeHolder<CraftingRecipe>` (`CraftingResultMixin` redirect-хендлер);
  - `EnchantmentHelper.getAvailableEnchantmentResults` — 3-й параметр стал
    `Stream<Holder<Enchantment>>` (был `boolean treasure`) (`EnchantmentHelperMixin`);
  - `BrewingStandBlockEntity.isBrewable` получил параметр `PotionBrewing`
    (`BrewingStandMixin` redirect).
- ⚠️ **Не до конца проверенные миксины (ленивая загрузка).** На главном меню
  применились 18 микстинов; `EnchantmentHelperMixin` (в `EnchantmentHelper`)
  применяется только при первом зачаровании — формально подтвердить ретестом.
- ⚠️ **Анонимные классы могли сместиться.** `GrindstoneMenuMixin` целится в
  `GrindstoneMenu$4` — на 1.21 применился, но при будущих обновлениях номер
  перепроверять по байткоду.
- ⚠️ **`@ModifyConstant` на `40` в `AnvilMenuMixin`** (игнор «Too Expensive»):
  в `createResult` 1.21 три вхождения `40` — ordinal'ы могли сдвинуться; проверить
  поведение «дорогого» ремонта на наковальне у Зачарователя ур.8+.
- ⚠️ **Идентичность `Holder<MobEffect>` как ключа карты** (`BrewXPMap`/`PotionTierMap`):
  полагаемся на то, что `MobEffects.X` и `MobEffectInstance.getEffect()` дают один и
  тот же registry-holder. Для ванильных эффектов верно; проверить XP/тиры зелий вживую.
- Неиспользуемые импорты (`Map`/`Enchantment`/`SimpleContainer` и т.п.) после
  переписывания дают **warnings**, не ошибки — почистить при случае.

---

## 6. План тестирования (полный ретест после миграции)

Цель — подтвердить, что КАЖДАЯ механика пережила переезд. Запуск: `./gradlew runClient`,
новый мир (творческий — для выдачи предметов; проверять и в выживании, где нужно
урон/прочность). Прокачку выдавать через `/krp setlevel <path 0-4> <level>` и
очки тратить в меню (клавиша **K**). Заголовок окна должен быть «Kingdom RP».

### A. Инфраструктура / каркас
- [ ] Мод грузится, в `debug.log` есть `Registering mixin config` + `Mixing ... into ...` (18+), без `FATAL`.
- [ ] `/krp stats`, `/krp debug`, `/krp addxp`, `/krp setlevel`, `/krp reset` работают.
- [ ] Прокачка пути в HUD: при получении XP сверху появляется **полоска прогресса**;
      при левел-апе — жёлтая + звук; в чат — сообщение.
- [ ] Меню путей (K) и выбор специализации: список, трата очков, сообщение об улучшении.
- [ ] Данные **сохраняются** между перезаходами (выйти в меню → зайти; уровни на месте).
- [ ] Данные **переживают смерть** (умереть → возродиться; прогресс не сброшен — `copyOnDeath`).
- [ ] Синк клиент↔сервер: значения в `/krp debug` и в меню совпадают сразу после входа.

### B. Путь Добыча
- [ ] **Шахтёр**: XP за руды/камень; гейт добычи по тиру (низкий уровень — блок не
      ломается + сообщение); скорость добычи (+5%/ур); двойной дроп руды.
- [ ] **Лесоруб**: XP за брёвна/обтёсывание; гейт по тиру; скорость рубки (брёвна И
      обработанное дерево); сохранение прочности топора; двойной дроп; **срубка дерева
      целиком** с ур.5.
- [ ] Натуральные блоки vs поставленные: за поставленный блок XP/бонусов НЕТ.

### C. Путь Война
- [ ] **Воин**: XP за убийства/урон/получение урона; гейт ношения брони/оружия по
      `ItemUseTierMap` (низкий уровень — броня снимается + сообщение); +урон, −получаемый урон.
- [ ] **Лучник**: XP за попадание стрелой; урон лука по уровню; игнор брони ур.5+;
      **двойная стрела** ур.5+ (`createArrow` 4-арг — проверить, что стрела вылетает);
      ускоренная зарядка (`SpecializationEffects.onArrowLoose`, +3%/ур; слабое
      натяжение остаётся слабым — `BowItemMixin` удалён как сломанный/дублирующий).

### D. Путь Промысел
- [ ] **Фермер**: XP за урожай/посадку/костную муку/вспашку (`ItemAbilities.HOE_TILL`)/ягоды ПКМ;
      гейт посадки и добычи; двойной дроп; прочность мотыги; **комбайн** ур.5;
      **усиленная костная мука** (`isValidBonemealTarget` без boolean); **разведение**
      животных (гейт + XP), доение/стрижка.
- [ ] **Рыбак**: XP-маппинг улова; прочность удочки; двойной улов; XP за морскую флору;
      ускоренный клёв (`FishingHookMixin`).
- [ ] **Повар**: гейт крафта еды (`SlotMixin.mayPickup` — результат виден, но не берётся);
      гейт готовки в печи/коптильне (вход `CookGatedInputSlot`) и на костре (`applyCampfireGate`);
      XP за готовку (`FurnaceResultSlotMixin`).

### E. Путь Ремесло
- [ ] **Гейт крафта** (`SlotMixin.mayPickup`): для недоступного предмета результат
      ВИДЕН, но НЕ забирается ничем (клик/shift/Q/свап) + сообщение; ингредиенты целы.
- [ ] **Плотник**: XP/лестница доступа; экономия древесины (`CraftingInput`-рецепт),
      партия досок/палок, **строительный размах** (`BLOCK_INTERACTION_RANGE` +1/+2).
- [ ] **Кузнец**: XP крафта/плавки/незерит-апгрейда/ремонта; гейт крафта; **гейт
      переплавки** (вход печи); **закалка** прочности (`CraftingResultMixin` — проверить
      и обычный клик, и **shift-click**); незерит-апгрейд (`SmithingMenuMixin`) без
      понижения прочности; скидка ремонта на наковальне.
- [ ] **Мастеровой**: XP/лестница доступа натуральных материалов; экономия материала;
      партия блоков; **закалка кожаной брони** (клик и shift); обжиг (`NaturalSmeltMap`).

### F. Путь Магия (Data Components — главный риск)
- [ ] **Алхимик**: варка зелий стартует только при доступном уровне (гейт
      `BrewingStandMixin` — `level.potionBrewing()`); тиры зелий (`PotionTierMap` —
      `PotionContents`); XP за варку по тиру (`BrewXPMap`); экономия реагента;
      long/strong/splash/lingering модификаторы корректно повышают требование/XP.
- [ ] **Зачарователь** (после первого открытия стола — применится `EnchantmentHelperMixin`):
      - стол НЕ предлагает чары выше тира (фильтр пула `EnchantmentHelperMixin`);
      - число вариантов на столе по уровню (1/2/3) + затемнение/блок клика (`client.EnchantmentScreenMixin`);
      - гейт клика по запертому слоту/тиру; XP за чары (`EnchantXPMap` — по весу);
      - бонусы: усиление чары (+1 ур., через `updateEnchantments`), экономия лазурита, сохранение опыта;
      - наковальня: гейт книги по уровню, шанс успеха/провал, скидка уровней,
        игнор «Too Expensive» ур.8+ (`AnvilMenuMixin`);
      - точильный камень: XP за снятые НЕ-проклятые чары (`GrindstoneMenuMixin`, тег `CURSE`);
      - книга через `STORED`/обычные через `ENCHANTMENTS` — XP и тиры считаются верно.
- [ ] **Дебафф смерти**: после смерти эффект «Штраф к опыту» накладывается, XP режется;
      тултип эффекта показывает описание (`client.EffectTooltipMixin`); ⚠️ молоко его теперь СНИМАЕТ.

### G. Регресс-проверки конкретно под 1.21
- [ ] Закалка при **shift-click** (грабли №15) — броня/инструмент выходят с пониженной прочностью и при shift.
- [ ] Заголовок окна = «Kingdom RP» (`client.MinecraftTitleMixin`).
- [ ] Нет ошибок `Holder`/`PotionContents`/`ItemEnchantments` в логе при варке/зачаровании.
- [ ] Ресинк инвентаря при отменах (надевание брони не по уровню, гейт костра) — без фантомов.

> По итогам ретеста: отметить расхождения, занести найденные баги сюда и в
> «Частые ошибки» `KINGDOM_RP_ARCHITECTURE.md`. После зелёного ретеста — мердж
> ветки `migrate/1.21.1` в `main`.
