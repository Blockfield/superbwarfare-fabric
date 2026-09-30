# Брифинг для агентов порта Superb Warfare на Fabric 1.21.1

Форк: `superbwarfare-fabric`, ветка `fabric-port`. Апстрим — NeoForge 21.1.x, тот же
Minecraft 1.21.1, те же маппинги (officialMojangMappings + parchment). **Ванильные имена
не меняются** — переименовывать `net.minecraft.*` не нужно никогда.

## Железные правила

1. **Никогда не обращайся к API, существование которого не проверил.** Уже дважды порт ломался
   на выдуманных методах. Проверка без gradle:
   ```
   ./api.sh net.minecraft.world.item.ItemStack            # все сигнатуры
   ./api.sh net.minecraft.world.item.ItemStack burn       # только строки с 'burn'
   ```
   Оговорка: у сторонних зависимостей (Accessories, GeckoLib, Porting Lib) javap покажет
   ванильные типы intermediary-именами (`class_1309` = LivingEntity) — это нормально, читай
   имена методов. Инъекции интерфейсов Fabric API (`ItemStack.getRecipeRemainder()`) в jar
   ванильного minecraft не видны, но существуют — их ищи в `fabric-*-api-v1` jar'ах.
2. **Не запускай gradle.** 13 ГБ RAM, агенты работают параллельно. Сборку гоняет координатор.
3. `mixins.superbwarfare.json` общий для всех агентов — только **дописывай** строки, не
   переписывай файл целиком.
4. Не трогай файлы вне своего списка. Если правка требует общего шима — опиши это в отчёте,
   координатор сведёт.

## Шимы, которые уже есть (используй, не изобретай заново)

`src/main/kotlin/com/atsuishio/superbwarfare/fabric/`:

- `DeferredRegister.kt` — `DeferredRegister`/`DeferredHolder` поверх ванильного Registry.
  Форма вызова сохранена: `ModItems.ITEMS.register("name") { ... }` работает как в Forge.
- `ModEventBus.kt` — собственная шина для событий **самого SBW** + `open class CancellableEvent`.
  `postEvent(SomeEvent())` рассылает, `ModEventBus.subscribe<SomeEvent> { }` подписывает.
- `MultipartEntities.kt` — многосоставные хитбоксы (EnderDragonPart + Porting Lib PartEntity).
- `Accessories.kt` — адаптер Curios → Accessories (wispforest).
- `Capabilities.kt` + `src/main/java/.../fabric/*.java` — IEnergyStorage, IItemHandler,
  ItemStackHandler, InvWrapper, SidedInvWrapper, RecipeWrapper, SlotItemHandler,
  ItemHandlerHelper, EnergyStorage. **Java здесь намеренно**: Kotlin синтезирует свойства
  только из Java-геттеров, поэтому `handler.energyStored` продолжает работать.

## Три вида событий — не путай

| Что                                                                                                                                                                                            | Где живёт                                       | Как портируется                                  |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- | ------------------------------------------------ |
| События **самого SBW** (`ReloadEvent`, `RegisterContainersEvent` и т.п.)                                                                                                                       | код мода                                        | `ModEventBus`: `postEvent(...)` + `subscribe<T>` |
| События **Fabric API** (`ServerTickEvents`, `ClientTickEvents`, `HudRenderCallback`, `WorldRenderEvents`, `CommandRegistrationCallback`, `ServerPlayConnectionEvents`, `LootTableEvents`, ...) | fabric-api                                      | регистрируешь колбэк в `init()`                  |
| События **Porting Lib** (форджевой формы: `LivingHurtEvent` и др.)                                                                                                                             | `io.github.fabricators_of_create.porting_lib.*` | оставляешь как есть, меняется только импорт      |

Событий, которых нет ни там ни там (AnvilUpdateEvent, ExplosionEvent.Detonate,
RenderGuiLayerEvent, RenderNameTagEvent, LivingHealEvent, ItemEntityPickupEvent и др.),
**не выдумывай** — оставь `// ponytail: нужен свой миксин` и вынеси в отчёт.

## Грабли Kotlin, на которые уже наступали

- `object` — ключевое слово Kotlin, а Fabric API держит пакет
  `net.fabricmc.fabric.api.object.builder.v1.*`. Нужны бэктики: ``net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder``.
- Блочные комментарии Kotlin **вложенные**: `/*` внутри KDoc (например `slot/*.json`)
  открывает вложенный комментарий и съедает остаток файла. Не пиши `/*` в тексте комментария.
- Kotlin-интерфейсы не хранят состояние — событие с флагом `canceled` обязано быть `open class`.
- Локальная переменная, затеняющая параметр (`val context = ClipContext(...)` при параметре
  `context: WorldRenderContext`), даёт лавину непонятных ошибок. Переименовывай локальную.

## Файл с ошибками

`PORT-ERRORS.txt` в корне репозитория — полный вывод компилятора, `путь:строка:колонка сообщение`.
Свои строки бери оттуда: `grep 'твой/файл' PORT-ERRORS.txt`.

## Отчёт

Коротко: какие файлы дочинил, где оставил `ponytail:`-заглушку и почему, какие общие шимы
пришлось бы тронуть.
