package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.config.client.KillMessageConfig
import com.atsuishio.superbwarfare.event.KillMessageHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.ResourceLocationSerializer
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.LivingKillRecord
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

@Serializable
data class LivingGunKillMessage(
    val attackerId: Int,
    val targetId: Int,
    val headshot: Boolean,
    @Serializable(DamageTypeResourceKeySerializer::class)
    val damageType: ResourceKey<DamageType>,
    // Blockfield: far players are not tracked by the client, so ids alone drop the message.
    val attackerUuid: SerializedUUID,
    val targetUuid: SerializedUUID,
    val weapon: SerializedResourceLocation,
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        val level = clientLevel ?: return
        val entity =
            level.getEntity(attackerId) as? LivingEntity
                ?: KillMessageHandler.standIn(attackerUuid)?.also {
                    it.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(BuiltInRegistries.ITEM.get(weapon)))
                }
                ?: return

        val attacker =
            entity.takeIf {
                entity is Player || entity is OwnableEntity && entity.owner is Player
            } ?: return

        val target = level.getEntity(targetId) ?: KillMessageHandler.standIn(targetUuid) ?: return

        if (KillMessageHandler.QUEUE.size >= KillMessageConfig.KILL_MESSAGE_COUNT.get()) {
            KillMessageHandler.QUEUE.poll()
        }

        KillMessageHandler.QUEUE.offer(
            LivingKillRecord(
                attacker,
                target,
                attacker.mainHandItem,
                headshot,
                damageType,
            ),
        )
    }
}

private object DamageTypeResourceKeySerializer : KSerializer<ResourceKey<DamageType>> {
    override val descriptor = ResourceLocationSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: ResourceKey<DamageType>,
    ) {
        encoder.encodeSerializableValue(ResourceLocationSerializer, value.location())
    }

    override fun deserialize(decoder: Decoder): ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, decoder.decodeSerializableValue(ResourceLocationSerializer))
}
