package review

import com.atsuishio.superbwarfare.control.DroneControlSession

fun main() {
    var count = 0

    fun expect(
        value: Boolean,
        label: String,
    ) {
        check(value) { label }
        count++
    }

    val session = DroneControlSession("session-a")
    expect(!session.accept("session-b", 1), "wrong session id rejected")
    expect(!session.accept("session-a", -1), "negative sequence rejected")
    expect(session.accept("session-a", 0), "first sequence accepted")
    expect(!session.accept("session-a", 0), "duplicate sequence rejected")
    expect(!session.accept("session-a", 0), "duplicate rejection is idempotent")
    expect(session.accept("session-a", 1), "next sequence accepted")
    expect(!session.accept("session-a", 1), "replayed old sequence rejected")
    expect(!session.accept("session-b", 2), "correct sequence but stale/other session rejected")
    expect(session.accept("session-a", 5), "gaps are fine, only order matters")
    expect(!session.accept("session-a", 4), "cannot rewind after a gap")

    val defaulted = DroneControlSession()
    val other = DroneControlSession()
    expect(defaulted.id != other.id, "auto-generated ids are distinct")

    println("Session checks: $count passed")
}
