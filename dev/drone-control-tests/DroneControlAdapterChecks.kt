package review

import com.atsuishio.superbwarfare.control.DroneControlAccess
import com.atsuishio.superbwarfare.control.DroneControlEvents
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.TestHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TestServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

fun main() {
    var count = 0

    fun expect(
        value: Boolean,
        label: String,
    ) {
        check(value) { label }
        count++
    }
    val server = TestServer()
    val world = ServerLevel(server)
    val otherWorld = ServerLevel(server)
    val player = ServerPlayer(world, "owner")
    val other = ServerPlayer(world, "other")
    server.players["owner"] = player
    server.players["other"] = other
    val drone = DroneEntity(world, "drone")
    world.drones["drone"] = drone
    drone.entityData.set(DroneEntity.LINKED, true)
    drone.entityData.set(DroneEntity.CONTROLLER, "owner")

    fun monitor(id: String = "drone") =
        ItemStack(ModItems.MONITOR.get()).also {
            it.data.putBoolean(MonitorItem.LINKED, true)
            it.data.putBoolean(MonitorItem.USING, true)
            it.data.putString(MonitorItem.LINKED_DRONE, id)
        }

    fun armed() {
        drone.processInput(511)
        drone.mouseInput(9.0, -4.0)
        drone.fire = true
    }

    fun cleared() = drone.keys == 0.toShort() && drone.mouseX == 0.0 && drone.mouseY == 0.0 && !drone.fire
    player.mainHandItem = monitor()
    expect(DroneControlAccess.resolve(player) === drone, "resolve valid owner")
    other.mainHandItem = monitor()
    expect(DroneControlAccess.resolve(other) == null, "copied monitor cannot control another owner's drone")
    armed()
    DroneControlAccess.stopMonitor(other, drone)
    expect(!cleared(), "other owner cannot reset inputs")
    expect(other.mainHandItem.data.getBoolean(MonitorItem.USING), "other owner stack not mutated")
    player.isAlive = false
    expect(DroneControlAccess.resolve(player) == null, "dead owner rejected")
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(cleared(), "death clears all input channels")
    expect(!player.mainHandItem.data.getBoolean(MonitorItem.USING), "death stops monitor")
    expect(player.resetPackets == 1, "death sends camera reset")
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(player.resetPackets == 1, "cleanup idempotent")
    player.isAlive = true
    player.mainHandItem = monitor()
    player.isSpectator = true
    expect(DroneControlAccess.resolve(player) == null, "spectator rejected")
    player.isSpectator = false
    player.isRemoved = true
    expect(DroneControlAccess.resolve(player) == null, "removed player rejected")
    player.isRemoved = false
    player.moveWorld(otherWorld)
    expect(!DroneControlAccess.canUse(player, drone), "different world rejected")
    // Dimension-change teardown: the operator is in another world entirely (not just too far in
    // the same world). getController() must still find them server-wide so the drone's tick loop
    // (resetIfUncontrolled) actually calls stopMonitor/sends the camera reset, instead of seeing
    // "no controller" and silently leaving the operator's monitor stuck on Using=true.
    armed()
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(cleared(), "dimension change clears drone input")
    expect(!player.mainHandItem.data.getBoolean(MonitorItem.USING), "dimension change stops the operator's monitor")
    player.moveWorld(world)
    player.mainHandItem = monitor()
    player.pos = Vec3(150.0, 0.0, 0.0)
    expect(DroneControlAccess.resolve(player) === drone, "configured range inclusive")
    player.pos = Vec3(150.01, 0.0, 0.0)
    expect(DroneControlAccess.resolve(player) == null, "out of range rejected")
    player.pos = Vec3(0.0, 0.0, 0.0)
    player.mainHandItem = monitor("different")
    expect(!DroneControlAccess.canUse(player, drone), "exact entity binding")
    player.mainHandItem = ItemStack(Item())
    expect(DroneControlAccess.resolve(player) == null, "non-monitor rejected")
    armed()
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(cleared(), "switching away clears inputs")
    player.mainHandItem = monitor()
    drone.isRemoved = true
    expect(DroneControlAccess.resolve(player) == null, "removed drone rejected")
    drone.isRemoved = false
    drone.isAlive = false
    expect(DroneControlAccess.resolve(player) == null, "dead drone rejected")
    drone.isAlive = true
    drone.entityData.set(DroneEntity.LINKED, false)
    expect(DroneControlAccess.resolve(player) == null, "unlinked drone rejected")
    drone.entityData.set(DroneEntity.LINKED, true)
    player.mainHandItem.data.putBoolean(MonitorItem.USING, false)
    expect(DroneControlAccess.canUse(player, drone, false), "valid activation")
    expect(DroneControlAccess.resolve(player) == null, "inactive monitor cannot send input")
    player.mainHandItem = monitor()
    armed()
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(!cleared(), "active control preserved")
    val otherDrone = DroneEntity(world, "second")
    otherDrone.entityData.set(DroneEntity.CONTROLLER, "owner")
    otherDrone.entityData.set(DroneEntity.LINKED, true)
    DroneControlAccess.stopMonitor(player, otherDrone)
    expect(player.mainHandItem.data.getBoolean(MonitorItem.USING), "old drone cannot stop another monitor")
    expect(!cleared(), "dormant binding does not cancel active drone")
    server.players.remove("owner")
    DroneControlAccess.resetIfUncontrolled(drone)
    expect(cleared(), "missing/disconnected owner clears inputs")
    server.players["owner"] = player

    // Run the actual production callback bodies with a minimal event-bus test double.
    DroneControlEvents.onInitialize()

    fun tick() {
        ServerTickEvents.START_WORLD_TICK.listeners.forEach { it(world) }
    }

    fun load() {
        ServerEntityEvents.ENTITY_LOAD.listeners.forEach { it(drone, world) }
    }

    fun unload() {
        ServerEntityEvents.ENTITY_UNLOAD.listeners.forEach { it(drone, world) }
    }
    player.mainHandItem = monitor()
    armed()
    load()
    expect(cleared(), "load resets persisted input")
    armed()
    tick()
    expect(!cleared(), "loaded active drone retains control")
    player.isAlive = false
    tick()
    expect(cleared(), "tick lifecycle clears dead owner")
    player.isAlive = true
    player.mainHandItem = monitor()
    armed()
    ServerPlayConnectionEvents.DISCONNECT.listeners.forEach { it(TestHandler(player), server) }
    expect(cleared(), "disconnect callback resets inputs")
    expect(!player.mainHandItem.data.getBoolean(MonitorItem.USING), "disconnect does not auto-resume monitor")
    player.mainHandItem = monitor()
    armed()
    unload()
    expect(cleared(), "unload resets inputs")
    expect(!player.mainHandItem.data.getBoolean(MonitorItem.USING), "unload stops view")
    armed()
    player.isAlive = false
    tick()
    expect(!cleared(), "unloaded entity is not retained by lifecycle")
    load()
    armed()
    ServerLifecycleEvents.SERVER_STOPPED.listeners.forEach { it(server) }
    tick()
    expect(!cleared(), "stopped server no longer retained")

    // Control-session anti-replay wired through the same resetInput/resolve choke points above.
    player.mainHandItem = monitor()
    val sessionA = drone.beginControlSession()
    expect(DroneControlAccess.acceptsSequence(drone, sessionA, 0), "fresh session accepts sequence 0")
    expect(!DroneControlAccess.acceptsSequence(drone, sessionA, 0), "replayed sequence rejected")
    expect(!DroneControlAccess.acceptsSequence(drone, "stale-session", 1), "packet naming a foreign session rejected")
    expect(DroneControlAccess.acceptsSequence(drone, sessionA, 1), "next sequence in the same session accepted")
    DroneControlAccess.resetInput(drone)
    expect(!DroneControlAccess.acceptsSequence(drone, sessionA, 2), "resetInput invalidates the session")
    val sessionB = drone.beginControlSession()
    expect(sessionB != sessionA, "reactivation mints a new session id")
    expect(!DroneControlAccess.acceptsSequence(drone, sessionA, 2), "old session id never accepted again, even with a fresh sequence")
    expect(DroneControlAccess.acceptsSequence(drone, sessionB, 0), "new session starts its own sequence at 0")
    DroneControlAccess.stopMonitor(player, drone)
    expect(!DroneControlAccess.acceptsSequence(drone, sessionB, 1), "stopMonitor ends the session too")

    println("Adapter/lifecycle checks (test doubles): $count passed")
}
