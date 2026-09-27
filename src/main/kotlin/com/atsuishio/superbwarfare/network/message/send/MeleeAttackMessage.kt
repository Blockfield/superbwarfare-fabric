package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.sendPacketTo
import io.github.fabricators_of_create.porting_lib.entity.events.player.AttackEntityEvent
import kotlinx.serialization.Serializable
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import kotlin.math.*
import kotlin.random.Random

@Serializable
data class MeleeAttackMessage(val uuidList: List<SerializedUUID>) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator || !player.isAlive) return

        // The client picks the targets; the server only accepts a gun melee on entities within
        // that gun's reach (+1 block of lag slack, as vanilla attacks).
        // ponytail: no server melee cooldown; hurt() invulnerability (10 ticks) keeps a spamming
        // client close to the legit MeleeDuration cadence (15-20 ticks). Add one if melee damage grows.
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val data = GunData.from(stack)
        val reach = data.get(GunProp.MELEE_RANGE) + 1.0

        val entities = uuidList.distinct()
            .mapNotNull { EntityFindUtil.findEntity(player.level(), it.toString()) }
            .filter { it !== player && player.canInteractWithEntity(it, reach) }

        for (type in Perk.Type.entries) {
            val instances = data.perk.getInstances(type)
            instances.forEach { it.perk.onMeleeSwing(data, it, player) }
        }

        if (entities.isNotEmpty()) {
            attack(player, entities)
        }
        player.swing(InteractionHand.MAIN_HAND)
    }

    fun attack(attacker: Player, targets: List<Entity>) {
        var hurtCount = 0
        targets.forEachIndexed { index, target ->
            if (AttackEntityEvent(attacker, target).post()) return@forEachIndexed
            if (!target.isAttackable) return@forEachIndexed
            if (target.skipAttackInteraction(attacker)) return@forEachIndexed

            val damage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * max((10.0 - index) / 10.0, 0.1)
            if (damage <= 0) return@forEachIndexed

            var knockback = attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK)
            attacker.level().playSound(
                null,
                attacker.x,
                attacker.y,
                attacker.z,
                SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                attacker.soundSource,
                1.0f,
                1.0f
            )

            val currentHealth = (target as? LivingEntity)?.health ?: 0.0F
            // Приклад игнорирует броню: bypasses_armor у damage type
            val source = ModDamageTypes.causeMeleeAbsoluteDamage(attacker.level().registryAccess(), null, attacker)
            val canHurt = target.hurt(source, damage.toFloat())
            if (!canHurt) {
                attacker.level().playSound(
                    null,
                    attacker.x,
                    attacker.y,
                    attacker.z,
                    SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    attacker.soundSource,
                    1.0f,
                    1.0f
                )
            } else {
                hurtCount++

                val vec = attacker.deltaMovement
                if (attacker.isSprinting) {
                    knockback++
                }
                if (knockback > 0) {
                    if (target is LivingEntity) {
                        target.knockback(
                            knockback * 0.5,
                            sin(attacker.yRot * PI / 180.0),
                            -cos(attacker.yRot * PI / 180.0)
                        )
                    } else {
                        target.push(
                            -sin(attacker.yRot * PI / 180.0) * knockback / 2.0,
                            0.1,
                            cos(attacker.yRot * PI / 180.0) * knockback / 2.0
                        )
                    }

                    attacker.deltaMovement = vec.multiply(0.6, 1.0, 0.6)
                    attacker.isSprinting = false
                }

                if (target is ServerPlayer && target.hurtMarked) {
                    sendPacketTo(target, ClientboundSetEntityMotionPacket(target))
                    target.hurtMarked = false
                    target.deltaMovement = vec
                }

                if (index == 0) {
                    attacker.level().playSound(
                        null,
                        target,
                        ModSounds.MELEE_HIT.get(),
                        SoundSource.PLAYERS,
                        1f,
                        ((2 * Random.nextDouble() - 1) * 0.1f + 1.0f).toFloat()
                    )
                    attacker.crit(target)
                }

                attacker.setLastHurtMob(target)
                val level = attacker.level()
                if (target is LivingEntity && level is ServerLevel) {
                    EnchantmentHelper.doPostAttackEffects(level, target, source)
                }

                if (target is LivingEntity) {
                    attacker.awardStat(Stats.DAMAGE_DEALT, ((currentHealth - target.health) * 10.0F).roundToInt())
                }
            }

            if (hurtCount > 0) {
                attacker.sweepAttack()
            }
        }
    }
}