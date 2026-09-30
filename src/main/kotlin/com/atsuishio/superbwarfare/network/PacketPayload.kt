package com.atsuishio.superbwarfare.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

/**
 * Замена IPayloadContext из NeoForge. Из всего контекста сообщения используют только player()
 * (49 обращений через sender() и одно напрямую), поэтому поверхность ровно такая — и ни один
 * из 80 файлов сообщений при переезде не меняется.
 */
fun interface PayloadContext {
    fun player(): Player
}

sealed class PacketPayload : CustomPacketPayload {
    override fun type() = payloadTypeMap[this::class.java]!!

    abstract fun PayloadContext.handler()

    /** Вызов обработчика снаружи: расширение объявлено на контексте и иначе недоступно. */
    fun handle(context: PayloadContext) = with(this) { context.handler() }
}

abstract class ServerPacketPayload : PacketPayload() {
    fun PayloadContext.sender() = player() as ServerPlayer
}

abstract class ClientPacketPayload : PacketPayload()
