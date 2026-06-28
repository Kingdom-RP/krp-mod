# Kingdom RP — инструкции для Claude

Это мод для Minecraft **1.21.1 + NeoForge**, реализующий RPG-систему специализаций
(Kingdom RP). Полная архитектура описана в `docs/KINGDOM_RP_ARCHITECTURE.md` —
прочитай его в начале работы над проектом. История решений вынесена в
`docs/DECISIONS_LOG.md`, технические грабли — в `docs/TECH_GOTCHAS.md`.

## Главные правила

1. **Геймдизайн-решения предупреждай заранее.** Если изменение похоже на баг,
   но на самом деле — запланированное поведение или правка баланса, сначала
   спроси/предупреди пользователя, не исправляй молча.

2. **Формат специализаций.** При доработке любой специализации придерживайся
   схемы:
   - 1. Действия для получения опыта (только путь, не специализация — XP идёт
     к пути целиком)
   - 2. Маппинг опыта (таблица блок/моб/предмет → XP)
   - 3. Эффекты специализации (доступ к контенту по уровням + активные эффекты)
   - 4. Шансы/формулы для эффектов из п.3

3. **Используй enum `Path` и `Spec`**, а не магические числа/строки.
   `Path.MINING.index`, `Spec.MINER.id` и т.д.

4. **Маппинги — отдельные классы в `data/`**, не захардкоженные if/else в
   логике. Каждый новый тип данных (BlockEntry, CraftEntry, KillEntry,
   TierRequirement и т.п.) — отдельный record.

5. **Один обработчик на одно событие.** Если нужно несколько проверок
   на одно событие (например `BlockEvent.BreakEvent`) — один `@SubscribeEvent`
   метод (`net.neoforged.bus.api.SubscribeEvent`), который вызывает приватные
   подметоды (`checkMiner`, `checkFarmer` и т.д.), а не несколько отдельных
   `@SubscribeEvent` на одно событие.

6. **Натуральные блоки.** Любая механика начисления XP/эффектов от ломки
   блоков должна проверять `PlacedBlockTracker.isPlacedByPlayer(pos)` —
   за поставленные игроком блоки бонусы не положены.

7. **Код шли текстом**, если изменения небольшие (до ~150 строк) — не файлом.
   Файлом — только когда правишь сразу много файлов или объём большой.

8. **После любого значимого изменения геймплея/архитектуры** — обнови
   `KINGDOM_RP_ARCHITECTURE.md` (раздел "Текущий статус" и, если нужно,
   "История решений" или "Частые ошибки"). Делай это сам, без напоминаний.

9. **Короткие комментарии.** Пиши комментарии кратко и по делу, не раздувай:
   не нужно 5 строк комментария на 1 строку кода. Комментируй только **текущее
   состояние**, а не историю («раньше было X», «после миграции», «грабли №N»,
   «изменено потому что…») — этому место в `.md`-файлах (`KINGDOM_RP_ARCHITECTURE.md`),
   и то без злоупотребления, а **не в коде**. В коде — что/зачем делает строка
   сейчас, без хронологии правок.

## Окружение

- Windows, IntelliJ IDEA, **Java 21**, **NeoForge 1.21.1 (21.1.233)**.
- Сборка: **ModDevGradle** (`net.neoforged.moddev` 2.0.141) + Gradle 8.14.
  Зависимость: `net.neoforged:neoforge:21.1.233` (через `neoForge { version = ... }`).
  Реобфускации нет (рантайм на Mojmap / official mappings). Сборка/проверка:
  `./gradlew build`, запуск: `./gradlew runClient` (рабочая папка — `run/`).
- **Метаданные мода — `src/main/resources/META-INF/neoforge.mods.toml`** (НЕ
  `mods.toml`). Формат 1.21: `type="required"`, зависимость на modid `neoforge`.
- **Данные игрока — Data Attachment** (`registry/KRPAttachments.PLAYER_DATA`,
  `copyOnDeath`), доступ `player.getData(...)` / `setData(...)`; capability больше
  нет. **Сеть** — `CustomPacketPayload` + `StreamCodec` + `PayloadRegistrar`
  (`network/NetworkHandler` на `RegisterPayloadHandlersEvent`). **Конфиг** —
  `ModConfigSpec`. **HUD** — `LayeredDraw.Layer` через `RegisterGuiLayersEvent`.
  **Магия** — Data Components (`PotionContents`, `ItemEnchantments`,
  `Holder<Enchantment>`/`Holder<MobEffect>`).
- Mixin — без MixinGradle; refmap не требуется (Mojmap), `compatibilityLevel`
  `JAVA_21`, все микстины `remap = false`.
  - **Конфиг `kingdomrpcore.mixins.json` подключается НАТИВНО через `[[mixins]]`
    `config="..."` в `neoforge.mods.toml`** (на 1.21 это поддерживается; костыль
    1.20.1 с атрибутом манифеста `MixinConfigs` и ресурсным `MANIFEST.MF` УДАЛЁН).
  - Проверка применения — `run/logs/debug.log`: `Registering mixin config: ...`
    и `Mixing <X> ... into <target>`.
  - ⚠️ **Цели инжекта проверяются в РАНТАЙМЕ, не компилятором.** При смене
    сигнатуры ванильного метода между версиями миксин падает с
    `InvalidInjectionException`/`InvalidAccessorException` только при ЗАГРУЗКЕ
    целевого класса (часть классов грузится лениво — напр. `EnchantmentHelper`
    при первом зачаровании). Поэтому после правок миксинов недостаточно `build` —
    нужен `runClient` и заход в соответствующее меню.
- Конфиг сервера: `saves/<мир>/serverconfig/kingdomrpcore-server.toml`
