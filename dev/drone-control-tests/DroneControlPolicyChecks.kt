package review

import com.atsuishio.superbwarfare.control.DroneControlFacts
import com.atsuishio.superbwarfare.control.DroneControlPolicy

fun main() {
    var count = 0

    fun expect(
        value: Boolean,
        label: String,
    ) {
        check(value) { label }
        count++
    }
    val valid =
        DroneControlFacts(
            "operator",
            "operator",
            "drone",
            "drone",
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            10.0,
            150.0,
        )
    expect(DroneControlPolicy.allows(valid), "valid owner")
    val invalid =
        listOf(
            valid.copy(operatorId = ""),
            valid.copy(droneId = ""),
            valid.copy(controllerId = "other"),
            valid.copy(monitorDroneId = "other"),
            valid.copy(monitorPresent = false),
            valid.copy(monitorLinked = false),
            valid.copy(monitorUsing = false),
            valid.copy(droneLinked = false),
            valid.copy(operatorEligible = false),
            valid.copy(droneAlive = false),
            valid.copy(sameWorld = false),
            valid.copy(distance = -1.0),
            valid.copy(distance = 150.001),
            valid.copy(distance = Double.NaN),
            valid.copy(distance = Double.POSITIVE_INFINITY),
            valid.copy(maxDistance = 0.0),
            valid.copy(maxDistance = -1.0),
            valid.copy(maxDistance = Double.NaN),
            valid.copy(maxDistance = Double.POSITIVE_INFINITY),
        )
    invalid.forEachIndexed { i, f -> expect(!DroneControlPolicy.allows(f), "invalid fact $i") }
    expect(DroneControlPolicy.allows(valid.copy(distance = 0.0)), "zero distance")
    expect(DroneControlPolicy.allows(valid.copy(distance = 150.0)), "inclusive range")
    expect(DroneControlPolicy.allows(valid.copy(monitorUsing = false), false), "activation need not already be active")
    invalid.filter { it != valid.copy(monitorUsing = false) }.forEachIndexed { i, f ->
        expect(!DroneControlPolicy.allows(f, false), "activation still validates fact $i")
    }
    // None of the seven independent gates can be bypassed by another flag's value.
    for (bits in 0 until 128) {
        val f =
            valid.copy(
                monitorPresent = bits and 1 != 0,
                monitorLinked = bits and 2 != 0,
                monitorUsing = bits and 4 != 0,
                droneLinked = bits and 8 != 0,
                operatorEligible = bits and 16 != 0,
                droneAlive = bits and 32 != 0,
                sameWorld = bits and 64 != 0,
            )
        expect(DroneControlPolicy.allows(f) == (bits == 127), "gate matrix $bits")
    }
    for (v in listOf(0.0, -1.0, 1.0, Float.MAX_VALUE.toDouble())) {
        expect(DroneControlPolicy.validMouseInput(v, -v), "representable mouse input $v")
    }
    for (v in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE)) {
        expect(!DroneControlPolicy.validMouseInput(v, 0.0), "invalid mouse X $v")
        expect(!DroneControlPolicy.validMouseInput(0.0, v), "invalid mouse Y $v")
    }
    expect(DroneControlPolicy.validBlockTarget(0f, -64f, 30000000f), "ordinary block target")
    for (v in listOf(
        Float.NaN,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,
        Float.MAX_VALUE,
        Int.MAX_VALUE.toFloat(),
    )) {
        expect(!DroneControlPolicy.validBlockTarget(v, 0f, 0f), "invalid X target $v")
        expect(!DroneControlPolicy.validBlockTarget(0f, v, 0f), "invalid Y target $v")
        expect(!DroneControlPolicy.validBlockTarget(0f, 0f, v), "invalid Z target $v")
    }
    expect(DroneControlPolicy.validBlockTarget(Int.MIN_VALUE.toFloat(), 0f, 0f), "negative int boundary")
    // Автопривязка: берём только свободный монитор или монитор более старого своего дрона.
    expect(DroneControlPolicy.adoptsMonitor(false, false, false, false), "fresh monitor adopted")
    expect(DroneControlPolicy.adoptsMonitor(false, true, true, true), "older own drone yields monitor")
    expect(!DroneControlPolicy.adoptsMonitor(true, false, false, false), "active view kept")
    expect(!DroneControlPolicy.adoptsMonitor(false, true, true, false), "newer own drone keeps monitor")
    expect(!DroneControlPolicy.adoptsMonitor(false, true, false, true), "another operator's drone untouched")
    println("Policy checks: $count passed")
}
