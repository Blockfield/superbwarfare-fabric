package com.atsuishio.superbwarfare.entity.vehicle

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

class Key<T>(
    val initial: T,
)

class EntityData {
    private val values = mutableMapOf<Key<*>, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: Key<T>): T = if (values.containsKey(key)) values[key] as T else key.initial

    fun <T> set(
        key: Key<T>,
        value: T,
    ) {
        values[key] = value
    }
}

open class DroneEntity(
    world: Level,
    id: String,
) : Entity(world, id) {
    val entityData = EntityData()
    var maxControlDistance = 150.0
    var keys: Short = 0
    var mouseX = 0.0
    var mouseY = 0.0
    open var fire = false
    var resets = 0
    private var sessionSequence = -1L
    private var sessionSeed = 0

    fun processInput(value: Short) {
        keys = value
        if (value == 0.toShort()) resets++
    }

    fun mouseInput(
        x: Double,
        y: Double,
    ) {
        mouseX = x
        mouseY = y
    }

    // Cross-dimension: looks the operator up server-wide (like MinecraftServer.getPlayerList()),
    // not just among this drone's own level's players -- see EntityFindUtil.findPlayerAnywhere.
    fun getController() = (level() as? ServerLevel)?.server?.players?.get(entityData.get(CONTROLLER))

    fun beginControlSession(): String {
        sessionSeed++
        val id = "session-$sessionSeed"
        entityData.set(SESSION, id)
        sessionSequence = -1
        return id
    }

    fun endControlSession() {
        entityData.set(SESSION, "none")
        sessionSequence = -1
    }

    fun acceptControlSequence(
        sessionId: String,
        sequence: Long,
    ): Boolean {
        if (entityData.get(SESSION) == "none" || sessionId != entityData.get(SESSION) ||
            sequence < 0 || sequence <= sessionSequence
        ) {
            return false
        }
        sessionSequence = sequence
        return true
    }

    companion object {
        val CONTROLLER = Key("")
        val LINKED = Key(false)
        val SESSION = Key("none")
    }
}
