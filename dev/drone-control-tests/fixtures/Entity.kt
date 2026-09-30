package net.minecraft.world.entity

import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class Entity(
    private var world: Level,
    private val id: String,
) {
    var isAlive = true
    var isRemoved = false
    var pos = Vec3(0.0, 0.0, 0.0)

    fun level() = world

    fun moveWorld(value: Level) {
        world = value
    }

    fun getStringUUID() = id

    fun position() = pos
}
