package com.atsuishio.superbwarfare.client.util

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModAttachments
import com.atsuishio.superbwarfare.init.getData
import com.atsuishio.superbwarfare.init.setData
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

/**
 * Дельты PlayerVariables, чей пакет приехал раньше сущности игрока на клиенте (JOIN/респавн).
 * Без задержки такие пакеты выбрасывались, и счётчик патронов в HUD оставался нулевым
 * до смены измерения.
 */
object PendingPlayerVariables {
    private val pending = HashMap<Int, Map<Byte, Int>>()

    fun stash(
        entityId: Int,
        data: Map<Byte, Int>,
    ) {
        synchronized(pending) {
            val merged = pending[entityId]?.toMutableMap() ?: HashMap()
            merged.putAll(data)
            pending[entityId] = merged
        }
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (mc.level != null) {
                val entries: List<Pair<Int, Map<Byte, Int>>>
                synchronized(pending) {
                    if (pending.isEmpty()) return@register
                    entries = pending.map { it.key to it.value }
                    pending.clear()
                }
                for ((entityId, data) in entries) {
                    val entity = mc.level?.getEntity(entityId) ?: continue
                    val variables = entity.getData(ModAttachments.PLAYER_VARIABLE)
                    for ((type, value) in data) {
                        if (type == (-1).toByte()) {
                            variables.activeThermalImaging = value == 1
                        } else {
                            val types = Ammo.entries.toTypedArray()
                            if (type < types.size) {
                                types[type.toInt()].set(variables, value)
                            }
                        }
                    }
                    entity.setData(ModAttachments.PLAYER_VARIABLE, variables)
                }
            } else {
                synchronized(pending) { pending.clear() }
            }
        }
    }
}
