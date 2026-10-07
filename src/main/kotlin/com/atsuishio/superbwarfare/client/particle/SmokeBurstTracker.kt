package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.entity.projectile.MAX_SMOKE_AGE

/** A producer can be reloaded while its old particles remain in this client world. */
internal class SmokeBurstTracker {
    private val seen = HashMap<Long, Long>()
    private var cleanupAt = Long.MIN_VALUE

    fun accept(
        seed: Long,
        age: Int,
        now: Long,
    ): Boolean {
        if (age >= MAX_SMOKE_AGE) return false
        if (now >= cleanupAt) {
            seen.entries.removeIf { it.value <= now }
            cleanupAt = now + 20
        }
        if ((seen[seed] ?: Long.MIN_VALUE) > now) return false
        seen[seed] = now + (MAX_SMOKE_AGE - age.coerceAtLeast(0) + 1) / 2
        return true
    }
}
