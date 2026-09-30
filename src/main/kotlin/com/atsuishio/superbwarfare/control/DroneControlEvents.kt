package com.atsuishio.superbwarfare.control

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerLevel
import java.util.Collections
import java.util.IdentityHashMap

/** Server-thread-only, loaded entities only: no scans, chunk tickets or persistent world references. */
object DroneControlEvents : ModInitializer {
    private val loaded = IdentityHashMap<ServerLevel, MutableSet<DroneEntity>>()

    override fun onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is DroneEntity) {
                loaded
                    .getOrPut(world) {
                        Collections.newSetFromMap(IdentityHashMap<DroneEntity, Boolean>())
                    }.add(entity)
                // Inputs saved before an unload/restart must not replay when the entity loads.
                DroneControlAccess.resetInput(entity)
            }
        }
        ServerEntityEvents.ENTITY_UNLOAD.register { entity, world ->
            if (entity is DroneEntity) {
                entity.getController()?.let { DroneControlAccess.stopMonitor(it, entity) }
                DroneControlAccess.resetInput(entity)
                loaded[world]?.let {
                    it.remove(entity)
                    if (it.isEmpty()) loaded.remove(world)
                }
            }
        }
        ServerTickEvents.START_WORLD_TICK.register { world ->
            loaded[world]?.forEach { DroneControlAccess.resetIfUncontrolled(it) }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            // Invalidate even when the monitor is no longer eligible because the player is dead.
            loaded.values.forEach { drones ->
                drones.forEach { drone ->
                    if (DroneControlAccess.owns(player, drone)) {
                        DroneControlAccess.stopMonitor(player, drone, notifyClient = false)
                        DroneControlAccess.resetInput(drone)
                    }
                }
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            loaded.keys.removeIf { it.server === server }
        }
    }
}
