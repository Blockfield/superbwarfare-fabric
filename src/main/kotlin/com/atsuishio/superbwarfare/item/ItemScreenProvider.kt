package com.atsuishio.superbwarfare.item

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

interface ItemScreenProvider {
    @Environment(EnvType.CLIENT)
    fun getItemScreen(
        stack: ItemStack,
        player: Player,
        hand: InteractionHand,
    ): Screen?
}
