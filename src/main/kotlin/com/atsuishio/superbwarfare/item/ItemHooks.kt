package com.atsuishio.superbwarfare.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers

/**
 * Два хука IItemExtension из NeoForge, которых нет ни в Fabric API, ни в Porting Lib.
 * Всё остальное (onEntitySwing, shouldCauseReequipAnimation, supportsEnchantment, getBurnTime,
 * canDisableShield, isDamageable) закрыто интерфейсами porting_lib.item.extensions.
 *
 * Вызываются из ItemStackAttributeMixin и ItemEntityHurtMixin.
 */

/**
 * Замена `IItemExtension#getDefaultAttributeModifiers(ItemStack)`: модификаторы, зависящие от
 * самого стака (вес ствола, износ брони). Ваниль читает только компонент ATTRIBUTE_MODIFIERS,
 * поэтому подмена сделана миксином в `ItemStack.forEachModifier` — той же точке, откуда
 * LivingEntity собирает атрибуты и откуда строится подсказка предмета.
 */
interface StackAttributeItem {
    fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers

    /**
     * То, что апстрим брал из `super`. Порядок как в ваниле: пустой компонент означает, что
     * модификаторы лежат в самом предмете (ArmorItem 1.21.1 отдаёт защиту только оттуда).
     */
    fun baseAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val fromComponent = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
        return if (fromComponent.modifiers().isEmpty()) stack.item.defaultAttributeModifiers else fromComponent
    }
}

/**
 * Замена `IItemExtension#canBeHurtBy(ItemStack, DamageSource)`. Огнестойкость ваниль проверяет
 * сама, здесь остаются только дополнительные иммунитеты мода.
 */
interface DamageFilterItem {
    fun canBeHurtBy(
        stack: ItemStack,
        source: DamageSource,
    ): Boolean
}
