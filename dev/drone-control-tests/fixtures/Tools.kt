package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

object NBTTool {
    // Copy semantics deliberately model custom-data tags; mutating a read does not save it.
    fun getTag(stack: ItemStack) = stack.data.copy()

    fun saveTag(
        stack: ItemStack,
        tag: CompoundTag,
    ) {
        stack.data = tag.copy()
    }
}

object EntityFindUtil {
    fun findDrone(
        world: Level,
        id: String,
    ) = world.drones[id]
}

@Suppress("UNUSED_PARAMETER")
fun ServerPlayer.sendPacket(packet: ResetCameraTypeMessage) {
    resetPackets++
}
