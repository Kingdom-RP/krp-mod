# Технические грабли (НЕ повторять)

Сборник низкоуровневых ловушек NeoForge/Mixin, на которые уже наступали.
Связано с `KINGDOM_RP_ARCHITECTURE.md`.

## Эффект, не снимаемый молоком (curative-items)

- **Молоко в Forge/NeoForge снимает эффекты через `LivingEntity.curePotionEffects(stack)`,
  а не `removeAllEffects()`.** Каждый `MobEffectInstance` хранит список «исцеляющих
  предметов» (`getCurativeItems()`, по умолчанию = ведро молока). `curePotionEffects`
  убирает только те эффекты, для которых `isCurativeItem(stack)` истинно. Чтобы эффект
  НЕ снимался молоком (но молоко продолжало снимать всё ванильное), при наложении
  чистим список: `inst.getCurativeItems().clear()`. Пустой список persist-ится в NBT
  эффекта → иммунитет к молоку переживает релог.
  - ⚠️ В 1.21 `getCurativeItems()` УДАЛЁН. Защита перенесена на `EffectCure`:
    `XPSystem.onEffectRemove` (`MobEffectEvent.Remove`) отменяет удаление
    `DEATH_XP_PENALTY` при `EffectCures.MILK`.

## Слоты, машины, shift-click

13. **Для МАШИН (печь/блок-сущность) гейтить слот на ВХОДЕ (`mayPlace`), а не на
    выходе.** Гейт на ВЫХОДЕ у машины плох вдвойне: (а) **софтлок** — продукт готов,
    но его некому забрать, машина застревает; (б) **обход разрушением** — сломав
    блок, игрок получает содержимое в дроп мимо гейта. Правильно — гейтить
    **попадание сырья в слот** через `Slot.mayPlace(ItemStack)`: ванильный
    `moveItemStackTo` его уважает, поэтому ОДНА точка покрывает и ручную укладку, и
    shift-click.
    - ⚠️ **`Slot.mayPickup` покрывает ВСЕ способы изъятия, включая shift-click.**
      `AbstractContainerMenu.doClick` в ветке QUICK_MOVE проверяет `slot.mayPickup`
      и выходит ДО `quickMoveStack`. Клик/Q/double-click идут через `tryRemove`,
      тоже зовущий `mayPickup`. Поэтому для слота РЕЗУЛЬТАТА КРАФТА (не машина — нет
      софтлока/обхода) `mayPickup` достаточно одной точкой (`SlotMixin`).
    - Реализация для Повара: входной слот печи подменяется на `CookGatedInputSlot`
      через redirect `new Slot` в конструкторе `AbstractFurnaceMenu`. Игрока добыть
      непросто: **`@Inject` в конструктор ЗАПРЕЩЁН** Mixin'ом («Found @Inject
      targetting a constructor»). Решение — `@Redirect` на ЧТЕНИЕ поля
      `Inventory.player` (строка `this.level = inventory.player.level()` ДО создания
      слотов), кэшируем игрока, второй `@Redirect` на `new Slot` использует кэш.
    - Гейтинг ПКМ-действий (костёр и т.п.) обязан покрывать ОБЕ руки: гард
      `event.getHand() != MAIN_HAND` пропускает off-hand — гейт/XP считать ДО гарда
      по `event.getItemStack()`.

15. **`quickMoveStack` (shift-click) перемещает результат в инвентарь ДО вызова
    `Slot.onTake`.** Любая логика «решить успех/провал и при провале отобрать
    результат» в `onTake` для shift-click уже опоздала. Решение для наковальни:
    access-гейт на ВХОДЕ (`createResult` не создаёт результат), а shift-взятие
    блокируется отдельным мизином на `quickMoveStack` (`ItemCombinerMenuMixin`).

15б. **МОДИФИКАЦИЯ крафт-результата (закалка прочности) на `ItemCraftedEvent`
    теряется при shift-click** — при shift `quickMoveStack` перемещает результат
    отдельным стаком (через `split`) ДО `ItemCraftedEvent`. Решение: модифицировать
    стак результата при его **СБОРКЕ** — `@Redirect` на `ResultContainer.setItem` в
    `CraftingMenu.slotChangedCraftingGrid` (`CraftingResultMixin`). Эффекты, которые
    ДОБАВЛЯЮТ предметы (экономия/партия), на `ItemCraftedEvent` работают и для shift —
    баг только у тех, что МЕНЯЮТ сам стак результата.

14. **Анонимные классы Minecraft нумеруются по порядку ОБЪЯВЛЕНИЯ в исходнике,
    включая инициализаторы полей.** Слот результата точильного камня — НЕ
    `GrindstoneMenu$3`, а `$4`: поле `repairSlots = new SimpleContainer(2){...}`
    занимает `$1`. Проверять номер по байткоду (`javap -p`), а не угадывать.

12. **Отмена `PlayerInteractEvent.EntityInteract`/`RightClickItem` на сервере без
    ресинка = фантом у клиента.** Клиент предсказывает взаимодействие (ведро→молоко,
    надевание брони по ПКМ) ДО ответа сервера. После `event.setCanceled(true)` вызвать
    `player.containerMenu.sendAllDataToRemote()` — принудительный ресинк инвентаря.
    (LMB-путь надевания брони чинится сам через `onEquipmentChange`.)

## Mixin / события

0. **Клиентский `@EventBusSubscriber` без `value = Dist.CLIENT`** падает на
   выделенном сервере (`Attempted to load class .../KeyMapping for invalid dist
   DEDICATED_SERVER`). Любой класс в `client/` с MOD-bus подписчиком обязан иметь
   `value = Dist.CLIENT`. На клиенте баг не виден — всплывает при `runServer`.

1. **`event.getX() instanceof Y y`** где `getX()` уже возвращает тип `Y` —
   компилятор ругается «subtype of pattern type». Решение: `Y y = event.getX();`.

4. **`@Shadow` не работает для полей родительского класса** (`inputSlots` в
   `AnvilMenu`, объявлено в `ItemCombinerMenu`). Решение — отдельный
   `@Mixin`-интерфейс с `@Accessor` на родительский класс.

5. **`getDamageAfterArmorAbsorb`** в `LivingEntity` — `protected static`, нельзя
   вызвать напрямую. Считать игнор брони через `Attributes.ARMOR` /
   `Attributes.ARMOR_TOUGHNESS` цели.

6. **Inject `@At("TAIL")` в метод с несколькими `return`** срабатывает только на
   ПОСЛЕДНЕМ. Для конкретной ветки — `@At("RETURN", ordinal=N)`, считая `return`'ы
   по декомпилированному байткоду.

7. **Конфиг-зависимые маппинги** нельзя инициализировать в статическом блоке, если
   используются значения `ModConfigSpec` — конфиг ещё не загружен на class-loading.
   Решение: ленивая `init()` (+ пересчёт на `ModConfigEvent`).

8. **Bonus drop / fellTree через `playerDestroy` внутри `BlockEvent.BreakEvent`**
   может рекурсивно вызвать события — выполнять через `server.execute()` (след. тик).

10. **`@Redirect` на ЧТЕНИЕ поля внутри `field -= expr`** не даёт изменить поле:
    JVM грузит старое значение на стек ДО `expr`, потом пишет `старое − результат`,
    затирая запись из хендлера. Решение — перехватывать **запись** (`opcode=PUTFIELD`).
    Альтернатива (чище) — MixinExtras `@ModifyExpressionValue` на чтение: принимает
    `(int original)`, инстанс через `this` (пример: `TideFishingHookMixin`).

11. **`@Shadow` поля родительского класса при `remap=false` без refMap не находится**
    (`@Shadow field X was not located`). Пример: `random` объявлен в `Entity`, миксин
    на `FishingHook`. Решения: (а) не шедоуить, добыть значение иначе; (б) `@Accessor`-
    интерфейс на родительский класс (как №4). Поля самого целевого класса шедоуятся.

## Экраны / клиент

9. **Динамические списки кнопок (PathScreen/SpecializationScreen)** — считать высоту
   окна и Y-позиции ДО рендера (в `init()`/helper), переиспользовать в `render()`.

- **Замыленные экраны в 1.21.** `Screen.render` САМ зовёт `renderBackground` →
  `renderBlurredBackground`. Переопределять `renderBlurredBackground` и
  `renderMenuBackground` пустыми, затемнение — своё `graphics.fill(...)`. Переопределять
  именно подметоды, а не сам `render`.

- **Кастомизация главного меню.** `TitleScreen` грузится сразу (миксин НЕ ленивый).
  Удаление кнопок Realms/копирайта — через `ScreenEvent.Init.Post` +
  `event.removeListener(...)`, **НЕ `@Shadow Screen.removeWidget`** (на унаследованный
  метод шадоу не подцепился: `InvalidMixinException`).

## Сборка

2. **Gradle-версия и плагины сборки** — не соглашаться вслепую на предложение IntelliJ
   обновить Gradle: ModDevGradle/тулчейн привязаны к конкретным версиям.
