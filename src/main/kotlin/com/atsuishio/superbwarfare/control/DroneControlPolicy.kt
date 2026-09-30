package com.atsuishio.superbwarfare.control

/** Server-derived facts only; never deserialize this object from a client payload. */
data class DroneControlFacts(
    val operatorId: String,
    val controllerId: String,
    val droneId: String,
    val monitorDroneId: String,
    val monitorPresent: Boolean,
    val monitorLinked: Boolean,
    val monitorUsing: Boolean,
    val droneLinked: Boolean,
    val operatorEligible: Boolean,
    val droneAlive: Boolean,
    val sameWorld: Boolean,
    val distance: Double,
    val maxDistance: Double,
)

object DroneControlPolicy {
    fun allows(
        facts: DroneControlFacts,
        requireUsing: Boolean = true,
    ): Boolean =
        with(facts) {
            operatorId.isNotBlank() && droneId.isNotBlank() &&
                operatorId == controllerId && droneId == monitorDroneId &&
                monitorPresent && monitorLinked && (!requireUsing || monitorUsing) &&
                droneLinked && operatorEligible && droneAlive && sameWorld &&
                distance.isFinite() && distance >= 0.0 &&
                maxDistance.isFinite() && maxDistance > 0.0 && distance <= maxDistance
        }

    /** Автопривязка на тике: свободный монитор (или монитор более старого дрона того же
     * оператора) переходит к этому дрону. Активный вид (`monitorUsing`) не перебиваем, чужой
     * дрон не отбираем -- вызывающая сторона перебирает только инвентарь своего оператора. */
    fun adoptsMonitor(
        monitorUsing: Boolean,
        linkedDroneAlive: Boolean,
        linkedDroneSameOperator: Boolean,
        linkedDroneOlder: Boolean,
    ): Boolean =
        !monitorUsing &&
            (!linkedDroneAlive || (linkedDroneSameOperator && linkedDroneOlder))

    // VehicleEntity.mouseInput narrows doubles to floats; finite doubles alone are insufficient.
    fun validMouseInput(
        x: Double,
        y: Double,
    ): Boolean = x.isFinite() && y.isFinite() && x.toFloat().isFinite() && y.toFloat().isFinite()

    fun validBlockTarget(
        x: Float,
        y: Float,
        z: Float,
    ): Boolean = validBlockCoordinate(x) && validBlockCoordinate(y) && validBlockCoordinate(z)

    private fun validBlockCoordinate(value: Float): Boolean =
        value.isFinite() && value.toDouble() >= Int.MIN_VALUE.toDouble() &&
            value.toDouble() <= Int.MAX_VALUE.toDouble()
}

/** Anti-replay for VehicleMovementMessage/MouseMoveMessage/DroneFireMessage: minted once per
 * monitor activation (DroneEntity.beginControlSession). A packet naming an old session, or an
 * old/duplicate sequence within the current session, must not move the drone. Never constructed
 * from client-supplied fields; only the id is echoed back to the client via synced entity data.
 */
class DroneControlSession(
    val id: String =
        java.util.UUID
            .randomUUID()
            .toString(),
) {
    private var sequence: Long = -1

    fun accept(
        sessionId: String,
        candidate: Long,
    ): Boolean {
        if (sessionId != id || candidate < 0 || candidate <= sequence) return false
        sequence = candidate
        return true
    }
}
