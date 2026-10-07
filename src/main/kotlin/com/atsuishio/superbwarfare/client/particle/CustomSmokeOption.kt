package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.ksp.annotation.GenerateMapCodec
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
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
class CustomSmokeOption(
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
                Vec3(random.nextGaussian() * spread, 0.0, random.nextGaussian() * spread),
                Vec3(random.nextGaussian() * speed, random.nextGaussian() * speed, random.nextGaussian() * speed),
                random.nextInt(200) + 600,
                // Upstream size: the vanilla quad size roll, scaled 10x.
                (random.nextFloat() * 0.5f + 0.5f) * 2f,
            )
        }
    }

    companion object {
        val STREAM_CODEC = createStreamCodec<CustomSmokeOption>()
    }
}
