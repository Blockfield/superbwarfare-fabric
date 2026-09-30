package com.atsuishio.superbwarfare.inventory.handler

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.fabric.ItemStackHandler
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack

open class VehicleContainerHandler(
    size: Int,
    val vehicle: VehicleEntity,
) : ItemStackHandler(size) {
    override fun onContentsChanged(slot: Int) {
        this.vehicle.setChanged()
    }

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ): Boolean = this.vehicle.canPlaceItem(slot, stack)

    open fun clear() {
        this.stacks.clear()
    }

    fun getItems(): NonNullList<ItemStack> = this.stacks

    fun setItems(list: NonNullList<ItemStack>) {
        this.stacks = list
    }
}
