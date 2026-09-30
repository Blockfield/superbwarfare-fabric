package net.minecraft.world.phys

import kotlin.math.sqrt

data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    fun distanceTo(v: Vec3): Double = sqrt((x - v.x) * (x - v.x) + (y - v.y) * (y - v.y) + (z - v.z) * (z - v.z))
}
