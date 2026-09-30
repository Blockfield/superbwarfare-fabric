package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.sounds.SoundEvent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity

@Suppress("FunctionName")
interface DamageAccess {
    fun `superbWarfare$getDeathSound`(): SoundEvent?

    fun `superbWarfare$getSoundVolume`(): Float

    fun `superbWarfare$playHurtSound`(pSource: DamageSource?)

    fun `superbWarfare$actuallyHurt`(
        pDamageSource: DamageSource?,
        pDamageAmount: Float,
    )

    fun `superbWarfare$hurtHelmet`(
        pDamageSource: DamageSource?,
        pDamageAmount: Float,
    )

    fun `superbWarfare$checkTotemDeathProtection`(pDamageSource: DamageSource?): Boolean

    /** LivingEntity#lastHurt: protected, а форсированный урон обязан его сбрасывать. */
    fun `superbWarfare$getLastHurt`(): Float

    fun `superbWarfare$setLastHurt`(value: Float)

    companion object {
        fun of(living: LivingEntity): DamageAccess = living as DamageAccess
    }
}
