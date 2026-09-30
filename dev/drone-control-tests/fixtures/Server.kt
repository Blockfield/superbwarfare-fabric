package net.minecraft.server.level

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

// Mirrors MinecraftServer.getPlayerList(): authoritative across every dimension, unlike a
// Level's own entity tracking which only knows about players currently in that one level.
class TestServer {
    val players = mutableMapOf<String, Player>()
}

class ServerLevel(
    val server: TestServer,
) : Level()

class ServerPlayer(
    world: Level,
    id: String,
) : Player(world, id) {
    var resetPackets = 0
}
