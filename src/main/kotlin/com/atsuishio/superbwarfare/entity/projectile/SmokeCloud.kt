package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.Collections
import java.util.IdentityHashMap

/** Emitted bursts, not a reconstruction from the producer's current position or unloaded tickCount. */
internal class SmokeCloud {
    internal class Burst(
        val born: Long,
        val pos: Vec3,
        val option: CustomSmokeOption,
    ) {
        val served = smokeReceiverSet<ServerPlayer>()

        fun age(now: Long) = (now - born).coerceIn(0, (MAX_SMOKE_AGE / 2).toLong()).toInt() * 2
    }

    internal val bursts = ArrayList<Burst>()

    fun emit(
        now: Long,
        pos: Vec3,
        option: CustomSmokeOption,
    ) {
        expire(now)
        check(bursts.size < 101) { "Smoke producer exceeded its 101-burst lifecycle" }
        bursts.add(Burst(now, pos, option.copy(age = 0)))
    }

    fun expire(now: Long) {
        bursts.removeIf { it.age(now) >= MAX_SMOKE_AGE }
    }

    fun tick(level: ServerLevel) {
        expire(level.gameTime)
        val players = level.players()
        val present = smokeReceiverSet<ServerPlayer>().apply { addAll(players) }
        for (burst in bursts) {
            // Forget respawned player instances as well as players who left this dimension.
            burst.served.retainAll(present)
            for (player in players) {
                if (player in burst.served) continue
                val pos = burst.pos
                if (!player.blockPosition().closerToCenterThan(pos, 512.0)) continue
                if (level.sendParticles(
                        player,
                        burst.option.copy(age = burst.age(level.gameTime)),
                        true,
                        pos.x,
                        pos.y,
                        pos.z,
                        0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                ) {
                    burst.served.add(player)
                }
            }
        }
    }

    fun save(tag: CompoundTag) {
        val list = ListTag()
        for (burst in bursts) {
            list.add(
                CompoundTag().apply {
                    putLong("Born", burst.born)
                    putDouble("X", burst.pos.x)
                    putDouble("Y", burst.pos.y)
                    putDouble("Z", burst.pos.z)
                    putFloat("R", burst.option.red)
                    putFloat("G", burst.option.green)
                    putFloat("B", burst.option.blue)
                    putLong("Seed", burst.option.seed)
                    putInt("Count", burst.option.count)
                    putFloat("Spread", burst.option.spread)
                    putFloat("Speed", burst.option.speed)
                },
            )
        }
        tag.put("SmokeBursts", list)
    }

    fun load(tag: CompoundTag) {
        bursts.clear()
        val list = tag.getList("SmokeBursts", Tag.TAG_COMPOUND.toInt())
        for (index in 0 until minOf(list.size, 101)) {
            val saved = list.getCompound(index)
            bursts.add(
                Burst(
                    saved.getLong("Born"),
                    Vec3(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z")),
                    CustomSmokeOption(
                        saved.getFloat("R"),
                        saved.getFloat("G"),
                        saved.getFloat("B"),
                        0,
                        saved.getLong("Seed"),
                        saved.getInt("Count").coerceIn(1, 50),
                        saved.getFloat("Spread"),
                        saved.getFloat("Speed"),
                    ),
                ),
            )
        }
    }
}

internal const val MAX_SMOKE_AGE = 800

// Entity equality uses id, and respawn copies it to a different ServerPlayer instance. Both sets in retainAll
// must use identity or the old receiver survives pruning and suppresses replay to its replacement.
internal fun <T : Any> smokeReceiverSet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
