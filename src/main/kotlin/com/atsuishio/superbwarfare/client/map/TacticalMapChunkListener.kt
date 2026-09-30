package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.config.server.MapConfig
import io.github.fabricators_of_create.porting_lib.level.events.LevelEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.chunk.LevelChunk

object TacticalMapChunkListener {
    fun init() {
        ClientChunkEvents.CHUNK_LOAD.register { _, chunk -> onChunkLoad(chunk) }
        LevelEvent.Load.EVENT.register { event -> onLevelLoad(event.level) }
        LevelEvent.Unload.EVENT.register { event -> onLevelUnload(event.level) }
    }

    fun isEnabled(): Boolean =
        try {
            MapConfig.ENABLE_TACTICAL_MAP.get()
        } catch (_: Exception) {
            false
        }

    private fun onChunkLoad(chunk: LevelChunk) {
        if (!isEnabled()) return
        TacticalMapCache.queueChunkUpdate(chunk)
    }

    private fun onLevelLoad(level: LevelAccessor) {
        if (!isEnabled()) return
        if (level is ClientLevel) {
            val worldId = TacticalMapCache.getWorldIdentifier()
            val dim = level.dimension().location().toString()
            TacticalMapCache.initForDimension(dim, worldId)
        }
    }

    private fun onLevelUnload(level: LevelAccessor) {
        // Always clear — config may already be inaccessible during world
        // teardown, and stale data bleeds into the next world otherwise.
        if (level is ClientLevel) {
            TacticalMapCache.clear()
        }
    }
}
