package net.minecraft.world.entity.player

import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

open class Player(
    world: Level,
    id: String,
) : Entity(world, id) {
    var isSpectator = false
    var mainHandItem = ItemStack(Item())
}
