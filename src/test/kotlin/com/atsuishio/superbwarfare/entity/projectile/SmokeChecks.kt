package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import com.atsuishio.superbwarfare.client.particle.SmokeBurstTracker
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3

private fun CustomSmokeOption.layout() = puffs().map { listOf(it.offset, it.velocity, it.lifetime, it.quadSize) }

private fun burst(
    seed: Long,
    age: Int = 0,
) = CustomSmokeOption(1f, 1f, 1f, age, seed, 8, 0.075f, 0.08f)

private data class SmokeReceiver(
    val entityId: Int,
)

fun smokeChecks() {
    // Every client, and a late replay at any age, unfolds the same puffs from the burst seed.
    val seed = burstSeed(42L, 10)
    check(burst(seed).layout() == burst(seed, 300).layout())
    check(burst(seed).puffs().size == 8)
    check(burst(seed).puffs().all { it.lifetime in 600..799 })
    check(burst(seed).puffs().any { it.offset.y != 0.0 })
    check(CustomSmokeOption(1f, 1f, 1f, 0, seed, 50, 0f, 0.07f).puffs().all { it.offset.lengthSqr() == 0.0 })
    // Other bursts and other grenades differ.
    check(burst(burstSeed(42L, 12)).layout() != burst(seed).layout())
    check(burst(burstSeed(43L, 10)).layout() != burst(seed).layout())

    // Wire roundtrip covers every field and consumes the full buffer without reflective startup.
    val option = CustomSmokeOption(.1f, .2f, .3f, 798, Long.MIN_VALUE + 42, 50, .075f, .07f)
    val buf = FriendlyByteBuf(Unpooled.buffer())
    try {
        CustomSmokeOption.STREAM_CODEC.encode(buf, option)
        check(CustomSmokeOption.STREAM_CODEC.decode(buf) == option)
        check(buf.readableBytes() == 0)
        ByteBufEncoder(buf).encodeSerializableValue(CustomSmokeOption.serializer(), option)
        check(CustomSmokeOption.STREAM_CODEC.decode(buf) == option)
        CustomSmokeOption.STREAM_CODEC.encode(buf, option)
        check(ByteBufDecoder(buf).decodeSerializableValue(CustomSmokeOption.serializer()) == option)
        check(buf.readableBytes() == 0)
    } finally {
        buf.release()
    }

    // A moving source emits at two different places. Save/reload retains those places and world-time ages.
    val cloud = SmokeCloud()
    cloud.emit(100, Vec3(1.0, 2.0, 3.0), burst(seed))
    cloud.emit(102, Vec3(21.0, 12.0, 13.0), burst(seed + 1))
    val tag = CompoundTag()
    cloud.save(tag)
    val restored = SmokeCloud()
    restored.load(tag)
    check(restored.bursts.map { it.pos } == cloud.bursts.map { it.pos })
    check(restored.bursts.map { it.option } == cloud.bursts.map { it.option })
    check(restored.bursts.map { it.age(202) } == listOf(204, 200))
    restored.expire(500)
    check(restored.bursts.single().pos == Vec3(21.0, 12.0, 13.0))
    restored.expire(502)
    check(restored.bursts.isEmpty())

    // A reloaded producer cannot double a burst already present, but a rebuilt client world can replay it.
    val tracked = SmokeBurstTracker()
    check(tracked.accept(seed, 0, 100))
    check(!tracked.accept(seed, 120, 160))
    check(tracked.accept(seed + 1, 120, 160))
    check(SmokeBurstTracker().accept(seed, 120, 160))
    check(!tracked.accept(seed, 800, 500))
    check(!emits(201) && !emits(202) && emits(200))

    // Vanilla entities compare by id, and respawn reuses that id for a new player object.
    val prior = SmokeReceiver(7)
    val respawned = SmokeReceiver(7)
    check(prior == respawned && prior !== respawned)
    val served = smokeReceiverSet<SmokeReceiver>().apply { add(prior) }
    check(prior in served && respawned !in served)
    val present = smokeReceiverSet<SmokeReceiver>().apply { add(respawned) }
    served.retainAll(present)
    check(served.isEmpty())
    check(served.add(respawned))
    check(!served.add(respawned))
    present.add(prior)
    check(present.size == 2)
}
