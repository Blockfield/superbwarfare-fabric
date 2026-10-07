package com.atsuishio.superbwarfare.client.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import java.util.WeakHashMap
import kotlin.math.min

@Environment(EnvType.CLIENT)
open class CustomSmokeParticle protected constructor(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    puff: CustomSmokeOption.Puff,
    private val spriteSet: SpriteSet,
    rCol: Float,
    gCol: Float,
    bCol: Float,
    startAge: Int,
) : TextureSheetParticle(level, x + puff.offset.x, y + puff.offset.y, z + puff.offset.z) {
    init {
        this.setSize(0.4f, 0.4f)
        this.quadSize = puff.quadSize
        this.lifetime = puff.lifetime
        this.gravity = 0.001f
        this.hasPhysics = true
        this.xd = puff.velocity.x * 0.5
        this.yd = puff.velocity.y * 0.5
        this.zd = puff.velocity.z * 0.5
        this.setSpriteFromAge(spriteSet)
        this.rCol = rCol
        this.gCol = gCol
        this.bCol = bCol
        // Replay native gravity, friction, collisions and fading; a single drift move is not equivalent.
        // This uses the client's loaded terrain, not a historical snapshot of blocks.
        repeat(startAge.coerceIn(0, 800) / 2) {
            if (!this.removed) tick()
        }
    }

    @Environment(EnvType.CLIENT)
    class Provider(
        private val spriteSet: SpriteSet,
    ) : ParticleProvider<CustomSmokeOption> {
        private val replayed = WeakHashMap<ClientLevel, SmokeBurstTracker>()

        override fun createParticle(
            pType: CustomSmokeOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double,
        ): Particle? {
            if (!replayed.getOrPut(pLevel) { SmokeBurstTracker() }.accept(pType.seed, pType.age, pLevel.gameTime)) return null
            val engine = Minecraft.getInstance().particleEngine
            for (puff in pType.puffs()) {
                if (pType.age >= puff.lifetime) continue
                val particle = CustomSmokeParticle(pLevel, x, y, z, puff, this.spriteSet, pType.red, pType.green, pType.blue, pType.age)
                if (particle.isAlive) engine.add(particle)
            }
            return null
        }
    }

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT

    override fun tick() {
        super.tick()
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(min((this.age / 8) + 1, 8), 8))
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0)) {
            if (this.age >= this.lifetime - 60 && this.alpha > 0.01f) {
                this.alpha -= 0.015f
            }
        } else {
            this.remove()
        }
    }
}
