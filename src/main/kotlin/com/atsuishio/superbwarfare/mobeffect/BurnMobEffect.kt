package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.entity.mixin.persistentData
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacket
import io.github.fabricators_of_create.porting_lib.entity.events.living.MobEffectEvent
import io.github.fabricators_of_create.porting_lib.entity.events.tick.EntityTickEvent
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity

object BurnMobEffect : MobEffect(MobEffectCategory.HARMFUL, -12708330) {
    const val TAG_ATTACKER = "BurnAttacker"

    fun init() {
        MobEffectEvent.Added.EVENT.register { onEffectAdded(it) }
        MobEffectEvent.Expired.EVENT.register { onEffectExpired(it) }
        MobEffectEvent.Remove.EVENT.register { onEffectRemoved(it) }
        EntityTickEvent.Post.EVENT.register { onLivingTick(it) }
    }

    override fun applyEffectTick(
        entity: LivingEntity,
        amplifier: Int,
    ): Boolean {
        val attacker =
            if (!entity.persistentData.contains(TAG_ATTACKER)) {
                null
            } else {
                entity.level().getEntity(entity.persistentData.getInt(TAG_ATTACKER))
            }

        entity.forceHurt(
            ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), attacker),
            0.6f + (0.3f * amplifier),
        )
        entity.invulnerableTime = 0

        val level = attacker?.level() ?: return false
        val player = attacker as? ServerPlayer ?: return false
        if (level is ServerLevel) {
            level.playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)

            player.sendPacket(ClientIndicatorMessage(0, 5))
        }
        return true
    }

    override fun shouldApplyEffectTickThisTick(
        pDuration: Int,
        pAmplifier: Int,
    ): Boolean = pDuration % 20 == 0

    private fun onEffectAdded(event: MobEffectEvent.Added) {
        val living = event.entity
        val instance = event.effectInstance ?: return
        if (instance.effect.value() != ModMobEffects.BURN.value()) {
            return
        }

        living.forceHurt(
            DamageSource(
                living
                    .level()
                    .registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.IN_FIRE),
                event.effectSource,
            ),
            0.6f + (0.3f * instance.amplifier),
        )
        living.invulnerableTime = 0

        val source = event.effectSource
        if (source is LivingEntity) {
            living.persistentData.putInt(TAG_ATTACKER, source.id)
        }
    }

    private fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.value() == ModMobEffects.BURN.value()) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    private fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.value() == ModMobEffects.BURN.value()) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    private fun onLivingTick(event: EntityTickEvent.Post) {
        val living = event.entity as? LivingEntity ?: return
        if (living.hasEffect(ModMobEffects.BURN)) {
            living.remainingFireTicks = 2
        }
        if (living.isInWater) {
            living.removeEffect(ModMobEffects.BURN)
        }
    }
}
