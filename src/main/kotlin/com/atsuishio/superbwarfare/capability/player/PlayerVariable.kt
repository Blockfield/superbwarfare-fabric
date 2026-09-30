package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModAttachments
import com.atsuishio.superbwarfare.init.getData
import com.atsuishio.superbwarfare.init.hasData
import com.atsuishio.superbwarfare.init.setData
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import java.util.*
import java.util.function.Consumer

class PlayerVariable {
    private var old: PlayerVariable? = null

    @JvmField
    var ammo: MutableMap<Ammo, Int> = EnumMap(Ammo::class.java)
    var activeThermalImaging: Boolean = false

    fun sync(entity: Entity) {
        if (!entity.hasData(ModAttachments.PLAYER_VARIABLE)) return

        val newVariable = entity.getData(ModAttachments.PLAYER_VARIABLE)
        if (old != null && old == newVariable) return

        if (entity is ServerPlayer) {
            entity.sendPacket(PlayerVariablesSyncMessage(entity.id, compareAndUpdate()))
        }
    }

    fun watch(): PlayerVariable {
        this.old = this.copy()
        return this
    }

    fun forceUpdate(): MutableMap<Byte, Int> {
        val map = hashMapOf<Byte, Int>()

        for (type in Ammo.entries) {
            map[type.ordinal.toByte()] = type.get(this)
        }

        map[(-1).toByte()] = if (this.activeThermalImaging) 1 else 0

        return map
    }

    fun compareAndUpdate(): MutableMap<Byte, Int> {
        val map = hashMapOf<Byte, Int>()
        val old = (if (this.old == null) PlayerVariable() else this.old)!!

        for (type in Ammo.entries) {
            val oldCount = old.ammo.getOrDefault(type, 0)
            val newCount = type.get(this)

            if (oldCount != newCount) {
                map[type.ordinal.toByte()] = newCount
            }
        }

        if (old.activeThermalImaging != this.activeThermalImaging) {
            map[(-1).toByte()] = if (this.activeThermalImaging) 1 else 0
        }

        return map
    }

    fun writeToNBT(): CompoundTag {
        val nbt = CompoundTag()

        for (type in Ammo.entries) {
            type.set(nbt, type.get(this))
        }

        nbt.putBoolean("ActiveThermalImaging", activeThermalImaging)

        return nbt
    }

    fun readFromNBT(tag: CompoundTag) {
        for (type in Ammo.entries) {
            type.set(this, type.get(tag))
        }

        activeThermalImaging = tag.getBoolean("ActiveThermalImaging")
    }

    fun copy(): PlayerVariable {
        val clone = PlayerVariable()

        for (type in Ammo.entries) {
            type.set(clone, type.get(this))
        }

        clone.activeThermalImaging = this.activeThermalImaging

        return clone
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerVariable) return false

        for (type in Ammo.entries) {
            if (type.get(this) != type.get(other)) return false
        }

        return activeThermalImaging == other.activeThermalImaging
    }

    companion object {
        /** Хранится тем же тегом, что и на NeoForge, — чтобы старые миры читались без миграции. */
        @JvmField
        val CODEC: Codec<PlayerVariable> =
            CompoundTag.CODEC.xmap(
                { tag -> PlayerVariable().apply { readFromNBT(tag) } },
                PlayerVariable::writeToNBT,
            )

        fun init() {
            ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> onPlayerLoggedIn(handler.player) }
            ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer, _ -> onPlayerRespawn(newPlayer) }
            ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, _, _ ->
                onPlayerChangeDimension(player)
            }
            ServerPlayerEvents.COPY_FROM.register { original, newPlayer, _ -> clonePlayer(original, newPlayer) }
        }

        @JvmStatic
        fun modify(
            player: Player,
            consumer: Consumer<PlayerVariable>,
        ) {
            val cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch()
            consumer.accept(cap)
            cap.sync(player)
        }

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerVariable = entity.getData(ModAttachments.PLAYER_VARIABLE)

        private fun onPlayerLoggedIn(player: ServerPlayer) {
            player.sendPacket(PlayerVariablesSyncMessage(player.id, getOrDefault(player).compareAndUpdate()))
            // Пакет на JOIN может доехать до клиента раньше, чем там заспавнится сущность игрока,
            // и молча выбрасывается — счётчик патронов остаётся нулевым до смены измерения.
            // Повторная полная синхронизация с задержкой закрывает гонку.
            Mod.queueServerWork(20) {
                if (player.hasDisconnected()) return@queueServerWork
                player.sendPacket(PlayerVariablesSyncMessage(player.id, getOrDefault(player).forceUpdate()))
            }
        }

        private fun onPlayerRespawn(player: ServerPlayer) {
            player.sendPacket(PlayerVariablesSyncMessage(player.id, getOrDefault(player).compareAndUpdate()))
            Mod.queueServerWork(20) {
                if (player.hasDisconnected()) return@queueServerWork
                player.sendPacket(PlayerVariablesSyncMessage(player.id, getOrDefault(player).forceUpdate()))
            }
        }

        private fun onPlayerChangeDimension(player: ServerPlayer) {
            player.sendPacket(PlayerVariablesSyncMessage(player.id, getOrDefault(player).forceUpdate()))
        }

        private fun clonePlayer(
            oldPlayer: ServerPlayer,
            newPlayer: ServerPlayer,
        ) {
            val original = oldPlayer.getData(ModAttachments.PLAYER_VARIABLE)
            if (newPlayer.level().isClientSide()) return
            newPlayer.setData(ModAttachments.PLAYER_VARIABLE, original.copy())
        }
    }

    override fun hashCode(): Int {
        var result = activeThermalImaging.hashCode()
        result = 31 * result + (old?.hashCode() ?: 0)
        result = 31 * result + ammo.hashCode()
        return result
    }
}
