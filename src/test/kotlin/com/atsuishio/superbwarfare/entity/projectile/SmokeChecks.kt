package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption

private fun CustomSmokeOption.layout() = puffs().map { listOf(it.offset, it.velocity, it.lifetime, it.quadSize) }

private fun burst(
    seed: Long,
    age: Int = 0,
) = CustomSmokeOption(1f, 1f, 1f, age, seed, 8, 0.075f, 0.08f)

fun smokeChecks() {
    // Every client, and a late replay at any age, unfolds the same puffs from the burst seed.
    val seed = burstSeed(42L, 10)
    check(burst(seed).layout() == burst(seed, 300).layout())
    check(burst(seed).puffs().size == 8)
    check(burst(seed).puffs().all { it.lifetime in 600..799 })
    // Other bursts and other grenades differ.
    check(burst(burstSeed(42L, 12)).layout() != burst(seed).layout())
    check(burst(burstSeed(43L, 10)).layout() != burst(seed).layout())

    // Fuse out at tick 7: live bursts go on even ticks up to 200; a newcomer at tick 15 gets the ones before it.
    check(replayedTicks(7, 15) == listOf(8, 10, 12, 14))
    check(replayedTicks(0, 1) == listOf(0))
    check(replayedTicks(5, 5).isEmpty())
    check(replayedTicks(150, 400) == (150..200 step 2).toList())
    check(!emits(201) && !emits(202) && emits(200))
}
