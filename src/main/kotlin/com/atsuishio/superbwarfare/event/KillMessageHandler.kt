package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.tools.LivingKillRecord
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.player.Player
import java.util.*

object KillMessageHandler {
    val QUEUE: Queue<LivingKillRecord> = ArrayDeque()

    // Blockfield: upstream held a message for 80 ticks, too short to read mid-fight.
    const val HOLD_TICKS = 160

    // Blockfield: names and team colours for players the client does not track. Lives here, not in the
    // packet class, so the dedicated server never links client-only classes.
    fun standIn(uuid: UUID): Player? {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return null
        return mc.connection?.getPlayerInfo(uuid)?.let { RemotePlayer(level, it.profile) }
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { onClientTick() }
    }

    private fun onClientTick() {
        for (record in QUEUE) {
            if (record.freeze && record.tick >= 3) {
                continue
            }
            record.tick++
            if (record.fastRemove && record.tick >= HOLD_TICKS + 2 || record.tick >= HOLD_TICKS + 20) {
                QUEUE.poll()
            }
        }
    }
}
