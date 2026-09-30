package com.atsuishio.superbwarfare.init

import net.minecraft.world.item.Item

class Holder(
    private val item: Item,
) {
    fun get() = item
}

object ModItems {
    val MONITOR = Holder(Item())
}
