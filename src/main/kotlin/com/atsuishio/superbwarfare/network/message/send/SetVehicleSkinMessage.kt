package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.misc.VehicleKeyItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class SetVehicleSkinMessage(val entityId: Int, val skinId: String) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator || !player.isAlive) return
        val vehicle = player.level().getEntity(entityId) as? VehicleEntity ?: return
        // The skin screen opens from SkinSprayItem.onInteractVehicle at melee range;
        // only accept the change that close. Locked vehicles still need the key
        // (same rule as VehicleEntity.interact).
        if (!player.canInteractWithEntity(vehicle, player.entityInteractionRange() + 1.0)) return
        if (vehicle.locked && player.mainHandItem.item !is VehicleKeyItem) return
        // Validate: blank = vanilla, otherwise skin must exist in VehicleSkin
        if (skinId.isBlank() || VehicleSkin.getSkins(vehicle.type).skins.any { it.id == skinId }) {
            vehicle.skinId = skinId
        }
    }
}
