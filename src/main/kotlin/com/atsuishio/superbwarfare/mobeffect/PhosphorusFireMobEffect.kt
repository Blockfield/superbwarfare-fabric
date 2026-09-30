package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.entity.mixin.persistentData
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.network.message.receive.ClientPhosphorusFireMessage
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingThis
import io.github.fabricators_of_create.porting_lib.entity.EffectCure
import io.github.fabricators_of_create.porting_lib.entity.events.living.MobEffectEvent
import io.github.fabricators_of_create.porting_lib.entity.events.tick.EntityTickEvent
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

object PhosphorusFireMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xB1C1F2) {
    const val TAG_PHOSPHORUS_FIRE_COUNT = "SbwPhosphorusFireCount"
    const val TAG_PHOSPHORUS_FIRE_ATTACKER = "SbwPhosphorusFireAttacker"

    fun init() {
        MobEffectEvent.Added.EVENT.register { onEffectAdded(it) }
        MobEffectEvent.Expired.EVENT.register { onEffectExpired(it) }
        MobEffectEvent.Remove.EVENT.register { onEffectRemoved(it) }
        EntityTrackingEvents.START_TRACKING.register { target, player -> onStartTracking(target, player) }
        EntityTickEvent.Post.EVENT.register { onLivingTick(it) }
    }

    override fun applyEffectTick(
        entity: LivingEntity,
        amplifier: Int,
    ): Boolean {
        val attacker =
            if (!entity.persistentData.contains(TAG_PHOSPHORUS_FIRE_ATTACKER)) {
                null
            } else {
                entity.level().getEntity(entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_ATTACKER))
            }

        val fireCount = entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_COUNT)
        val fireLevel = fireCount / 4

        var damage = 1f + 0.5f * amplifier + ((amplifier + 1) * 5f).coerceAtMost(fireLevel * (amplifier * 0.6f + 1.2f))
        if (entity.isInWater) {
            damage /= 1.5f
        }
        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            damage /= 2f
        }

        val enchantment =
            entity
                .level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.FIRE_PROTECTION)
                .orElse(null)
        if (enchantment != null) {
            val fireResLevel = EnchantmentHelper.getEnchantmentLevel(enchantment, entity)
            damage /= 1 + fireResLevel * 0.1f
        }

        entity.forceHurt(
            ModDamageTypes.causePhosphorusFireDamage(entity.level().registryAccess(), null, attacker),
            damage,
        )
        entity.invulnerableTime = 0
        entity.persistentData.putInt(TAG_PHOSPHORUS_FIRE_COUNT, fireCount + 1)
        return true
    }

    override fun shouldApplyEffectTickThisTick(
        pDuration: Int,
        pAmplifier: Int,
    ): Boolean = pDuration % 10 == 0

    override fun fillEffectCures(
        cures: Set<EffectCure?>,
        effectInstance: MobEffectInstance,
    ) {
    }

    private fun onEffectAdded(event: MobEffectEvent.Added) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.value() != ModMobEffects.PHOSPHORUS_FIRE.value()) {
            return
        }

        val source = event.effectSource
        if (source is LivingEntity) {
            living.persistentData.putInt(TAG_PHOSPHORUS_FIRE_ATTACKER, source.id)
        }

        living.sendPacketToTrackingThis(ClientPhosphorusFireMessage(living.id, true))
    }

    private fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.value() == ModMobEffects.PHOSPHORUS_FIRE.value()) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            living.sendPacketToTrackingThis(ClientPhosphorusFireMessage(living.id, false))
        }
    }

    private fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.value() == ModMobEffects.PHOSPHORUS_FIRE.value()) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            living.sendPacketToTrackingThis(ClientPhosphorusFireMessage(living.id, false))
        }
    }

    private fun onStartTracking(
        target: Entity,
        player: ServerPlayer,
    ) {
        if (target is LivingEntity) {
            if (target.hasEffect(ModMobEffects.PHOSPHORUS_FIRE)) {
                player.sendPacketToTrackingThis(ClientPhosphorusFireMessage(target.id, true))
            }
        }
    }

    private fun onLivingTick(event: EntityTickEvent.Post) {
        val living = event.entity as? LivingEntity ?: return
        if (!living.level().isClientSide && living.hasEffect(ModMobEffects.PHOSPHORUS_FIRE) &&
            living.level().gameTime % 1000 == 0.toLong()
        ) {
            event.entity.sendPacketToTrackingThis(ClientPhosphorusFireMessage(living.id, true))
        }
    }
}
