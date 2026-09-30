package com.atsuishio.superbwarfare.fabric

import io.wispforest.accessories.api.AccessoriesAPI
import io.wispforest.accessories.api.AccessoriesCapability
import io.wispforest.accessories.api.Accessory
import io.wispforest.accessories.api.slot.SlotEntryReference
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Тонкий слой поверх io.wispforest:accessories — замена Curios, которого под Fabric 1.21.1 нет.
 *
 * Соответствие типов:
 * - `CuriosApi.getCuriosInventory` -> [AccessoriesCapability.getOptionally]
 * - `ICuriosItemHandler.findFirstCurio` -> [AccessoriesCapability.getFirstEquipped]
 * - `SlotResult` -> [SlotEntryReference], `SlotContext` -> [SlotReference]
 * - `ICurioItem` -> [Accessory], `ICurioRenderer` -> `AccessoryRenderer`
 *
 * Слоты по-прежнему описываются данными, но в другом виде:
 * `data/<ns>/accessories/slot/` и `data/<ns>/accessories/entity/`,
 * тег предметов слота — `accessories:<slot>`.
 */

/** Аналог `CuriosApi.getCuriosInventory(entity)`. */
fun accessoriesOf(entity: LivingEntity?): AccessoriesCapability? = entity?.let { AccessoriesCapability.getOptionally(it).orElse(null) }

/** Аналог `ICuriosItemHandler.findFirstCurio(item)`; null, если не надет. */
fun findFirstEquipped(
    entity: LivingEntity?,
    item: Item,
): SlotEntryReference? = accessoriesOf(entity)?.getFirstEquipped(item)

/** Аналог `findFirstCurio(item).isPresent`. */
fun isAccessoryEquipped(
    entity: LivingEntity?,
    item: Item,
): Boolean = accessoriesOf(entity)?.isEquipped(item) == true

/** Все надетые аксессуары — замена обхода `equippedCurios` по индексам слотов. */
fun equippedAccessories(entity: LivingEntity?): List<SlotEntryReference> = accessoriesOf(entity)?.getAllEquipped() ?: emptyList()

/**
 * Замена Curios-идиомы `findFirstCurio(this).isEmpty` внутри `canEquip`: у Accessories
 * проверяемый стак уже привязан к слоту, поэтому нужен именно «надет ли ещё один экземпляр».
 */
fun isAnotherEquipped(
    stack: ItemStack,
    reference: SlotReference,
    item: Item,
): Boolean = reference.capability()?.isAnotherEquipped(stack, reference, item) == true

/** Аналог `SlotContext.visible()`: рендерится ли содержимое слота. */
val SlotReference.isSlotVisible: Boolean
    get() = slotContainer()?.shouldRender(slot()) ?: false

/**
 * Accessories не ищет аксессуары по `instanceof Accessory` — предмет надо положить в реестр явно.
 * Вызывается из [com.atsuishio.superbwarfare.init.ModItems.register] после регистрации предметов.
 */
fun registerAccessories(vararg items: Item) {
    for (item in items) {
        if (item is Accessory) AccessoriesAPI.registerAccessory(item, item)
    }
}
