package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.network.message.send.MeleeAttackMessage
import com.atsuishio.superbwarfare.tools.OBB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.sqrt

fun meleeReachChecks() {
    // A pickup-sized hull turned 45 degrees: its AABB corners are empty space.
    val hull = OBB(Vector3d(0.0, 1.0, 0.0), Vector3d(1.25, 0.75, 3.4), Quaterniond().rotateY(Math.toRadians(45.0)), OBB.Part.BODY)
    val aabb = OBB.getWorldAABB(hull)

    fun dist(
        eye: Vec3,
        withAabb: Boolean,
    ) = sqrt(MeleeAttackMessage.hitboxDistanceSqr(eye, aabb.takeIf { withAabb }, listOf(hull)))

    check(MeleeAttackMessage.hitboxDistanceSqr(Vec3(0.0, 1.0, 0.0), null, listOf(hull)) == 0.0)
    check(MeleeAttackMessage.hitboxDistanceSqr(Vec3(0.0, 1.0, 0.0), aabb, emptyList()) == 0.0)
    check(MeleeAttackMessage.hitboxDistanceSqr(Vec3.ZERO, null, emptyList()) == Double.MAX_VALUE)

    // 3 blocks straight out of the short side of the hull.
    val side = Vec3(1.0, 0.0, -1.0).normalize().scale(1.25 + 3.0).add(0.0, 1.0, 0.0)
    check(abs(dist(side, false) - 3.0) < 1e-6) { dist(side, false) }
    val distant = OBB(Vector3d(100.0, 1.0, 0.0), Vector3d(1.0, 1.0, 1.0), Quaterniond(), OBB.Part.BODY)
    check(abs(MeleeAttackMessage.hitboxDistanceSqr(side, null, listOf(distant, hull)) - 9.0) < 1e-6)

    // 1 block off the AABB corner is still > 4 blocks from the hull itself.
    val corner = Vec3(aabb.maxX + 0.7, 1.0, aabb.minZ - 0.7)
    check(dist(corner, true) < 1.0 + 1e-6)
    check(dist(corner, false) > 4.0) { dist(corner, false) }
}
