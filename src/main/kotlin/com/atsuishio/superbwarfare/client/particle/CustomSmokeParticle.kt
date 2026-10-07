package com.atsuishio.superbwarfare.client.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import kotlin.math.min
import kotlin.math.pow

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
        // Blockfield: a cloud replayed to a returning player must fade together with the original one.
        this.age = startAge
        val fading = startAge - (this.lifetime - 60)
        if (fading > 0) this.alpha = maxOf(0.02f, 1f - 0.015f * (fading / 2))
        // ...and sit where the original puff has drifted by now: velocity decays by friction every tick, and one
        // move() stops the whole drift at the floor and walls.
        if (startAge > 0) {
            val decay = this.friction.toDouble().pow(startAge / 2)
            val drift = (1 - decay) / (1 - this.friction)
            this.move(this.xd * drift, this.yd * drift, this.zd * drift)
            this.xd *= decay
            this.yd *= decay
            this.zd *= decay
        }
    }

    @Environment(EnvType.CLIENT)
    class Provider(
        private val spriteSet: SpriteSet,
    ) : ParticleProvider<CustomSmokeOption> {
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
            val engine = Minecraft.getInstance().particleEngine
            for (puff in pType.puffs()) {
                engine.add(
                    CustomSmokeParticle(pLevel, x, y, z, puff, this.spriteSet, pType.red, pType.green, pType.blue, pType.age),
                )
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
