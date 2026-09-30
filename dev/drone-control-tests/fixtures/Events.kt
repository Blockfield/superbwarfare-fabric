package net.fabricmc.fabric.api.event.lifecycle.v1

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TestServer
import net.minecraft.world.entity.Entity

class TestEvent<T> {
    val listeners = mutableListOf<T>()

    fun register(listener: T) {
        listeners.add(listener)
    }
}

object ServerEntityEvents {
    val ENTITY_LOAD = TestEvent<(Entity, ServerLevel) -> Unit>()
    val ENTITY_UNLOAD = TestEvent<(Entity, ServerLevel) -> Unit>()
}

object ServerTickEvents {
    val START_WORLD_TICK = TestEvent<(ServerLevel) -> Unit>()
}

object ServerLifecycleEvents {
    val SERVER_STOPPED = TestEvent<(TestServer) -> Unit>()
}
