package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.ksp.annotation.GenerateMapCodec
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3

/**
 * One burst of [count] smoke puffs. [age] is in CustomSmokeParticle age units (it ages 2 per tick); 0 for a freshly
 * emitted burst.
 *
 * Blockfield: vanilla particle packets let every client roll its own offsets, speeds and lifetimes, so the same
 * smoke covered different spots for different players. A burst is sent as one count=0 particle and every client
 * unfolds the same [puffs] from [seed]. [spread] is horizontal only: upstream's vertical spread was 0.01 blocks.
 */
@GenerateMapCodec
@Serializable
data class CustomSmokeOption(
    val red: Float,
    val green: Float,
    val blue: Float,
    val age: Int,
    val seed: Long,
    val count: Int,
    val spread: Float,
    val speed: Float,
) : ParticleOptions {
    override fun getType(): ParticleType<*> = ModParticleTypes.CUSTOM_SMOKE.get()

    class Puff(
        val offset: Vec3,
        val velocity: Vec3,
        val lifetime: Int,
        val quadSize: Float,
    )

    fun puffs(): List<Puff> {
        val random = RandomSource.create(seed)
        return List(count) {
            Puff(
                Vec3(random.nextGaussian() * spread, random.nextGaussian() * minOf(spread, 0.01f), random.nextGaussian() * spread),
                Vec3(random.nextGaussian() * speed, random.nextGaussian() * speed, random.nextGaussian() * speed),
                random.nextInt(200) + 600,
                // Upstream size: the vanilla quad size roll, scaled 10x.
                (random.nextFloat() * 0.5f + 0.5f) * 2f,
            )
        }
    }

    companion object {
        // Keep the eight-field wire order explicit; this particle needs no Kotlin reflection at startup.
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, CustomSmokeOption> =
            StreamCodec.of(
                { buf, option ->
                    buf.writeFloat(option.red)
                    buf.writeFloat(option.green)
                    buf.writeFloat(option.blue)
                    buf.writeVarInt(option.age)
                    buf.writeLong(option.seed)
                    buf.writeVarInt(option.count)
                    buf.writeFloat(option.spread)
                    buf.writeFloat(option.speed)
                },
                { buf ->
                    CustomSmokeOption(
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readVarInt(),
                        buf.readLong(),
                        buf.readVarInt(),
                        buf.readFloat(),
                        buf.readFloat(),
                    )
                },
            )
    }
}
