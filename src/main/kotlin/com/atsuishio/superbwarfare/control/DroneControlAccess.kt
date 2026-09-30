package com.atsuishio.superbwarfare.control

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

/** One authority check shared by the existing monitor and all four drone input handlers. */
object DroneControlAccess {
    fun resolve(player: Player): DroneEntity? {
        val stack = player.mainHandItem
        if (!stack.`is`(ModItems.MONITOR.get())) return null
        val tag = NBTTool.getTag(stack)
        if (!tag.getBoolean(MonitorItem.LINKED) || !tag.getBoolean(MonitorItem.USING)) return null
        val drone =
            EntityFindUtil.findDrone(player.level(), tag.getString(MonitorItem.LINKED_DRONE))
                ?: return null
        return drone.takeIf { canUse(player, it) }
    }

    fun owns(
        player: Player,
        drone: DroneEntity,
    ): Boolean = drone.entityData.get(DroneEntity.CONTROLLER) == player.getStringUUID()

    fun canUse(
        player: Player,
        drone: DroneEntity,
        requireUsing: Boolean = true,
    ): Boolean {
        val stack = player.mainHandItem
        val tag = NBTTool.getTag(stack)
        return DroneControlPolicy.allows(
            DroneControlFacts(
                operatorId = player.getStringUUID(),
                controllerId = drone.entityData.get(DroneEntity.CONTROLLER),
                droneId = drone.getStringUUID(),
                monitorDroneId = tag.getString(MonitorItem.LINKED_DRONE),
                monitorPresent = stack.`is`(ModItems.MONITOR.get()),
                monitorLinked = tag.getBoolean(MonitorItem.LINKED),
                monitorUsing = tag.getBoolean(MonitorItem.USING),
                droneLinked = drone.entityData.get(DroneEntity.LINKED),
                operatorEligible = player.isAlive && !player.isRemoved && !player.isSpectator,
                droneAlive = drone.isAlive && !drone.isRemoved,
                sameWorld = player.level() === drone.level(),
                distance = player.position().distanceTo(drone.position()),
                maxDistance = drone.maxControlDistance,
            ),
            requireUsing,
        )
    }

    fun resetInput(drone: DroneEntity) {
        // These are the real synced input fields, not unused persistentData keys.
        drone.processInput(0)
        drone.mouseInput(0.0, 0.0)
        drone.fire = false
        // Every existing teardown path (unload, stopMonitor, resetIfUncontrolled) routes through
        // here, so this is the single choke point that invalidates a control session too.
        drone.endControlSession()
    }

    /** Anti-replay: sessionId/sequence must name the drone's current session and strictly
     * advance it. Call once per VehicleMovementMessage/MouseMoveMessage/DroneFireMessage,
     * after resolve()/canUse() already confirmed live ownership.
     */
    fun acceptsSequence(
        drone: DroneEntity,
        sessionId: String,
        sequence: Long,
    ): Boolean = drone.acceptControlSequence(sessionId, sequence)

    /** Only touches this owner's active main-hand monitor; dormant copies cannot stop another drone. */
    fun stopMonitor(
        player: Player,
        drone: DroneEntity,
        notifyClient: Boolean = true,
    ) {
        if (!owns(player, drone)) return
        val stack = player.mainHandItem
        if (!stack.`is`(ModItems.MONITOR.get())) return
        val tag = NBTTool.getTag(stack)
        if (tag.getString(MonitorItem.LINKED_DRONE) != drone.getStringUUID()) return
        if (!tag.getBoolean(MonitorItem.USING)) return
        tag.putBoolean(MonitorItem.USING, false)
        NBTTool.saveTag(stack, tag)
        resetInput(drone)
        if (notifyClient && player is ServerPlayer) player.sendPacket(ResetCameraTypeMessage)
    }

    fun resetIfUncontrolled(drone: DroneEntity) {
        val player = drone.getController()
        if (player != null && canUse(player, drone)) return
        resetInput(drone)
        if (player != null) stopMonitor(player, drone)
    }
}
