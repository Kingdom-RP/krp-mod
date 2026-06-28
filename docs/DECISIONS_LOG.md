# История решений (хронология)

Датированные записи о доработках и фиксах. Текущее состояние систем — в
`KINGDOM_RP_ARCHITECTURE.md`; технические грабли — в `TECH_GOTCHAS.md`.

## Tide + Backpacks + локализация (2026-06-28)

- **Рыбалка переведена на мод Tide** (опц. интеграция `compat/TideCompat` +
  `TideMixinPlugin`). Tide заменяет ванильный крючок своим — удалены эффекты на
  ванильном крючке (двойной улов, прочность, luck, небо/вода) и `FishingHookMixin`.
- Ускоренный клёв под Tide: `TideFishingHookMixin` (`@ModifyExpressionValue` на
  чтение `lureSpeed` в `catchingFish`), +1 с 5 уровня Рыбака.
- XP за улов по редкости вида (читаем датапак Tide `fishing/fish` на reload):
  common 5 / uncommon 8 / rare 10 / very_rare 15 / legendary 25.
- Гейт снастей по уровню Рыбака (крафт наживок/крючков/лесок/удочек + использование
  удочек); готовка рыбы Tide — под Повара. OP-предметы отключены оверрайдом рецептов
  (`neoforge:false`, dep `ordering=AFTER`). Журнал новичку выключен в конфиге Tide.
- **Backpacks**: крафт рюкзака = Мастеровой 3, большого = 5; перевод предметов.
  Локализация биндинга меню путей. Фикс тултипа крафта в 2×2 (`InventoryMenu`).

## Контент 1.20.5–1.21.1 в маппингах + аудит (2026-06-28)

Маппинги собирались по 1.20; новый контент доведён по логике существующих карт:
- **Туф-семейство** → Мастеровой (кладка ур.0, `chiseled` ур.4).
- **Медь 1.21** (chiseled/grate/bulb/door/trapdoor + cut copper, все окисления/воск)
  → Кузнец, медь тир 1.
- **Булава (mace)** → ношение Воина ур.7. **Черепаший шлем** → крафт Мастеровой
  тир 2, ношение Воин ур.2.
- **Мобы**: breeze/bogged → Воин тир B; armadillo → Промысел + разведение Фермер ур.4.
- ⚠️ **Блоки-компрессии (9→1) НЕ маппим** — крафт даёт XP → разобрал обратно → абуз.
  Cut copper это не нарушает (1:1).
- Аудит всех 1255 предметов (diff `Items.java` против `Items.X`+`Blocks.X`).
  Пропущено: бетон (погружение пудры, не крафт-событие), кораллы (убраны из Промысла),
  редстоун/рельсы/beacon/conduit (политика), семена/саженцы/листва (гейт по блоку),
  сырьё-дропы (XP за руду/моба), crafter/vault/heavy core (не крафт/лут).

## Лесоруб: XP при гейте + биом-локаут (2026-06-20)

- **Баг: XP за заблокированную добычу.** Гейт тира (`checkTierRestriction`) и XP
  (`XPSystem.onBlockBreak`) — РАЗНЫЕ `@SubscribeEvent`. Фикс: гейту задан
  `EventPriority.HIGH`, а `XPSystem.onBlockBreak` в начале `if (event.isCanceled()) return;`.
  ⚠️ Два обработчика на одно событие в разных классах — порядок не гарантирован.
- **Баланс: оверворлд-деревья разгейчены** (тир-0 в `BlockTierMap`), иначе игрок в
  саванне/болоте не добудет местную древесину. Гейт только на незер-стеблях/гигантских
  грибах (ур.6).

## Идеи на будущее: статистика и логирование (бэклог, 2026-06-20)

Пока НЕ делаем. Хранилище прогресса — Data Attachment → NBT в `playerdata/`.
Аналитика — аддитивный слой поверх.
- **Логирование прогресса**: append-only event-log (CSV/JSON) в точках левел-апа.
- **Статистика по онлайну** — итерация `server.getPlayerList().getPlayers()`.
- **Статистика по всем (вкл. офлайн)** — вторичный индекс (SQLite/H2 «stats.db»),
  обновляемый на logout/левел-апе.
- Принцип: Data Attachment = источник истины; лог/БД = производная.

## Интеграция Farmer's Delight (мягкая, 2026-06-21)

Паттерн кросс-мод совместимости БЕЗ хард-зависимости:
- **Карты по ID** — `addById(String id, …)` (резолв `BuiltInRegistries.*.getOptional`,
  no-op без контента).
- **`compat/FarmersDelightCompat`** (`FMLCommonSetupEvent`, `enqueueWork`, гейт по
  `ModList.isLoaded`): культуры (Фермер), еда (Повар, тиры 1–10 по ценности), ножи (Кузнец).
- **Гейт + XP Cooking Pot** (`compat/mixin/CookingPotResultSlotMixin`, target по строке,
  `remap=false`): гейт в `remove(int)` → `ItemStack.EMPTY` при нехватке уровня; XP в
  `onTake` через `CookSystem.onCooked`. ⚠️ `CookingPotMealSlot` — слот-превью; реальное
  изъятие — `CookingPotResultSlot`. Отдельный конфиг `*.farmersdelight.mixins.json` +
  `FDMixinPlugin`.
- **Обобщён гард незрелости** (`XPSystem.isImmatureCrop`): ванильный `CropBlock` →
  `isMaxAge`, модовые → по свойству `age`.
- ⚠️ НЕ покрыто: Cutting Board (нарезка ножом — отдельная механика).

## Фермер: убран XP за посадку — эксплойт (2026-06-21)

XP капал от **посадки** (`WorldEvents.onBlockPlace`): семена возвращаются при ломке
ростка → посадка→ломка→пересадка = бесконечный фарм. Фикс: XP за посадку убран
(гейт посадки и трекинг клеток сохранены). Компенсация: HARVEST-XP культур в
`BlockXPMap` +1. `PlantEntry.plantXP` оставлен в данных, но не начисляется.

## Баланс голода (важность Повара / Добычи, 2026-06-20)

- **Возрождение с 50% голода** (`XPSystem.onPlayerRespawn`, конфиг
  `balance.respawnFoodLevel`=10), пропускается при `isEndConquered`.
- **Расход голода ×2** (`FoodDataMixin` → `@ModifyVariable` на `FoodData.addExhaustion`,
  `balance.exhaustionMultiplier`=2.0). ⚠️ Конфиг SERVER — до синка на клиенте
  `SPEC.isLoaded()` может быть false (защита от краша).
- **TODO**: уменьшить восстановление голода от еды (через мизин `FoodData.eat`/per-item).

## Анти-грифинг к релизу (2026-06-20)

- **Жёсткий бан крафта** (`BannedCraftMap` + `RestrictionSystem.isCraftBanned`,
  `antiGrief.craftBanEnabled`): TNT, вагонетка с TNT, кристалл Энда, воронка(+вагонетка),
  observer, поршни, раздатчик, выбрасыватель. Проверяется ПЕРЕД спец-гейтом; блокировка —
  `SlotMixin.mayPickup`.
- **Закрыт Энд** (`onTravelToDimension`, `EntityTravelToDimensionEvent`,
  `antiGrief.closeEnd`): отмена телепорта при `getDimension() == Level.END`.

## Команды /krp: путь по названию + цель-игрок (2026-06-20)

- Аргумент пути — **название** (`craft`/`harvest`/…) с автодополнением; индекс 0–4 тоже
  принимается (`KRPCommand.parsePath`).
- `addxp`/`setlevel`/`reset`/`stats`/`debug` — опц. `target` (`EntityArgument.player`),
  модиф. команды и цель-не-себя требуют `permission(2)`. Вывод через `sendSuccess`.

## Кастомизация главного меню (2026-06-20)

`client.TitleScreenMixin` (`@Mixin(TitleScreen)`, remap=false):
- `@Redirect BrandingControl.forEachLine` → пусто (скрыть строки версий).
- `@Redirect SplashRenderer.render` → пусто (временно скрыт splash).
- `@Redirect LogoRenderer.renderLogo` → blit `title_logo.png` (512×160), по центру,
  ширина 380. Подход как в легаси — картинка, не шрифт.

`ClientEvents.onTitleScreenInit` (`ScreenEvent.Init.Post`): удаляет Realms/копирайт
(скан `getListenersList()`), сдвигает виджеты вверх на 24 px (см. грабли в TECH_GOTCHAS).

## Серверная проверка модов клиента (2026-06-20)

Анти-чит белый список на хендшейке NeoForge 1.21 (config-фаза):
- `ModWhitelistConfigurationTask` (`ICustomConfigurationTask`) на каждое подключение
  (`RegisterConfigurationTasksEvent`) → шлёт `ModCheckRequestPayload`.
- Клиент отвечает `ModListReplyPayload` (свои `ModList.get().getMods()`).
- Сервер сверяет с **моды сервера ∪ `extraAllowedMods`**; лишние → `disconnect`.
- Пэйлоады **обязательные** (не optional) → ванильные/не-Neo клиенты отсекаются
  хендшейком. Конфиг (SERVER): `modCheck.enabled`, `modCheck.extraAllowedMods`.
- ⚠️ Список сообщает сам клиент — защита от честных/казуальных, не от глубокой модификации.
  `finishCurrentTask` бросает вне config-фазы; config-пэйлоады — `FriendlyByteBuf`-кодеки.

## Пострелизный ретест миграции 1.21 (2026-06-19) — фиксы

- **Замыленные экраны** — переопределить `renderBlurredBackground`/`renderMenuBackground`
  пустыми (см. TECH_GOTCHAS).
- **Молоко снимало штраф XP** — `EffectCure` через `XPSystem.onEffectRemove`
  (`getCurativeItems` в 1.21 нет).
- **Фермер качался на недозрелом** — `onBlockBreak` пропускает `CropBlock && !isMaxAge`.
- **Наковальня врала про уровень книги** — выводит реальное `krp$requiredFor`; убрано
  двойное сообщение (`createResult` на клиентском И серверном меню — слать при `!isClientSide`).
- **Лук**: `BowItemMixin` удалён (был сломан, дублировал `ArrowLooseEvent`). Новый перк
  Лучника — **дальность полёта стрелы** (`onArrowSpawn`, скорость ×(0.5 + 0.1·ур)).
  Визуал натяжения не меняется.
- **Закалка распространена**: дерево → Мастеровой (`CraftsmanTemperMap` 1/3), камень →
  Кузнец (`BlacksmithTemperMap` 1/4).

## Иконки (логотип мода + иконка окна)

- **Логотип мода**: `logoFile="logo.png"` в `neoforge.mods.toml` (128×128 PNG).
- **Иконка окна**: ваниль ставит её из корня game-jar — мод туда не пишет.
  Переустанавливаем сами: `WindowIcon.apply()` грузит PNG из `src/main/resources/icons/`
  и зовёт `GLFW.glfwSetWindowIcon` на `ClientModEvents.onClientSetup` (главный поток).
