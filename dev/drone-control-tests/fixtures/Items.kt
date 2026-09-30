package net.minecraft.world.item

import net.minecraft.nbt.CompoundTag

class Item

class ItemStack(
    val item: Item,
) {
    var data = CompoundTag()

    fun `is`(other: Item) = item === other
}
