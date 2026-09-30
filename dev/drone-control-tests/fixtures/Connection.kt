package net.fabricmc.fabric.api.networking.v1

import net.fabricmc.fabric.api.event.lifecycle.v1.TestEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TestServer

class TestHandler(
    val player: ServerPlayer,
)

object ServerPlayConnectionEvents {
    val DISCONNECT = TestEvent<(TestHandler, TestServer) -> Unit>()
}
