package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.network.message.receive.*
import com.atsuishio.superbwarfare.network.message.send.*
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

val payloadTypeMap = mutableMapOf<Class<*>, CustomPacketPayload.Type<*>>()

inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

/** Регистрация приёмников на клиенте откладывается: ClientPlayNetworking доступен только там. */
private val clientReceivers = mutableListOf<() -> Unit>()

private inline fun <reified T : PacketPayload> payloadType(): CustomPacketPayload.Type<T> {
    val className = T::class.java.simpleName.substringBefore("Message")

    val name = buildString {
        append(className[0].lowercase())

        for (i in 1 until className.length) {
            val c = className[i]
            if (c.isUpperCase()) {
                append("_")
            }
            append(className[i].lowercase())
        }
    }

    val type = CustomPacketPayload.Type<T>(loc(name))
    payloadTypeMap[T::class.java] = type
    return type
}

private inline fun <reified T : ServerPacketPayload> playToServer() {
    val type = payloadType<T>()
    val codec: StreamCodec<in RegistryFriendlyByteBuf, T> = createStreamCodec<T>()
    PayloadTypeRegistry.playC2S().register(type, codec)
    ServerPlayNetworking.registerGlobalReceiver(type) { payload, context ->
        payload.handle { context.player() }
    }
}

private inline fun <reified T : ClientPacketPayload> playToClient() {
    val type = payloadType<T>()
    val codec: StreamCodec<in RegistryFriendlyByteBuf, T> = createStreamCodec<T>()
    PayloadTypeRegistry.playS2C().register(type, codec)
    clientReceivers.add {
        ClientPlayNetworking.registerGlobalReceiver(type) { payload, context ->
            payload.handle { context.player() }
        }
    }
}

fun initializeNetwork() {
    registerPayloads()
}

/** Вызывается из клиентской точки входа после initializeNetwork. */
fun initializeClientNetwork() {
    clientReceivers.forEach { it() }
}

private fun registerPayloads() {
    playToClient<ClientIndicatorMessage>()
    playToClient<ClientSetMotionMessage>()
    playToClient<DataSyncMessage>()
    playToClient<ClientMotionSyncMessage>()
    playToClient<ClientPhosphorusFireMessage>()
    playToClient<ContainerDataMessage>()
    playToClient<DrawClientMessage>()
    playToClient<FinishAssemblingVehicleMessage>()
    playToClient<LivingGunKillMessage>()
    playToClient<PlayerVariablesSyncMessage>()
    playToClient<RadarMenuCloseMessage>()
    playToClient<RadarMenuOpenMessage>()
    playToClient<ResetCameraTypeMessage>()
    playToClient<BeyondVisualEntitySyncMessage>()
    playToClient<ExplosionParticleMessage>()
    playToClient<MissileTrailParticleMessage>()
    playToClient<ShakeClientMessage>()
    playToClient<ShootClientMessage>()
    playToClient<SoundClientMessage>()
    playToClient<VehicleShootClientMessage>()
    playToClient<TDMSyncMessage>()
    playToClient<EntityRelationSyncMessage>()
    playToClient<PlayerInfoSyncMessage>()
    playToClient<RadarSyncMessage>()
    playToClient<ClientVehicleItemMessage>()
    playToClient<OpenVehicleSkinScreenMessage>()
    playToClient<OpenTacticalMapScreenMessage>()

    playToServer<AdjustMortarAngleMessage>()
    playToServer<AdjustZoomFovMessage>()
    playToServer<AimVillagerMessage>()
    playToServer<AssembleVehicleMessage>()
    playToServer<ChangeVehicleSeatMessage>()
    playToServer<ArtilleryIndicatorFireMessage>()
    playToServer<DogTagFinishEditMessage>()
    playToServer<DoubleJumpMessage>()
    playToServer<DroneFireMessage>()
    playToServer<EditMessage>()
    playToServer<FireKeyMessage>()
    playToServer<FireModeMessage>()
    playToServer<FiringParametersEditMessage>()
    playToServer<GunReforgeMessage>()
    playToServer<InteractMessage>()
    playToServer<LungeMineAttackMessage>()
    playToServer<MeleeAttackMessage>()
    playToServer<MouseMoveMessage>()
    playToServer<ParachuteMessage>()
    playToServer<PlayerStopRidingMessage>()
    playToServer<RadarChangeModeMessage>()
    playToServer<RadarSetPosMessage>()
    playToServer<RadarSetTargetMessage>()
    playToServer<RadarSetParametersMessage>()
    playToServer<ReloadMessage>()
    playToServer<SeekingWeaponWarningMessage>()
    playToServer<SensitivityMessage>()
    playToServer<SetFiringParametersMessage>()
    playToServer<SetVehicleSkinMessage>()
    playToServer<SetPerkLevelMessage>()
    playToServer<ShootMessage>()
    playToServer<ShowChargingRangeMessage>()
    playToServer<SwitchScopeMessage>()
    playToServer<SwitchVehicleWeaponMessage>()
    playToServer<UnloadMessage>()
    playToServer<VehicleFireMessage>()
    playToServer<VehicleMovementMessage>()
    playToServer<WeaponZoomingMessage>()
    playToServer<ZoomMessage>()
    playToServer<BlueprintCraftMessage>()
    playToServer<BlueprintSetIndexMessage>()
    playToServer<LoiterConfigMessage>()
    playToServer<LoiterOverrideMessage>()
    playToServer<VehicleUnloadPassengersMessage>()
    playToServer<VehicleDisconnectTowingMessage>()
    playToServer<EntityClearMessage>()
    playToServer<EntityAreaClearMessage>()
}