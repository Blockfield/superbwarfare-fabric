package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.fabric.isAnotherEquipped
import io.wispforest.accessories.api.Accessory
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class ThermalImagingGogglesItem :
    Item(Properties().stacksTo(1)),
    Accessory {
    override fun canEquip(
        stack: ItemStack,
        reference: SlotReference,
    ): Boolean = !isAnotherEquipped(stack, reference, this)
}
