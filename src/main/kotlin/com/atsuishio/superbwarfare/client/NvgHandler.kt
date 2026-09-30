package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.mixins.GameRendererInvoker
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player

/**
 * ПНВ шлема: клавиша включает пост-шейдер трубки на клиенте (shaders/program/nvg.fsh). Ванильный
 * night_vision не вешаем — усиление света делает сам шейдер, поэтому глухая темнота остаётся
 * шумной темнотой, а фонари и вспышки засвечивают кадр. Серверу о ПНВ знать не нужно.
 */
@Environment(EnvType.CLIENT)
object NvgHandler {
    private val mc get() = Minecraft.getInstance()

    private var active = false

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ -> tick() }
    }

    private fun hasNvgHelmet(player: Player) = player.getItemBySlot(EquipmentSlot.HEAD).`is`(ModTags.Items.HAS_NVG)

    private fun tick() {
        val player = mc.player
        if (player == null) {
            active = false
            return
        }

        var toggled = false
        while (ModKeyMappings.TOGGLE_NVG.consumeClick()) {
            toggled = true
        }

        if (toggled && (active || hasNvgHelmet(player))) {
            setActive(!active)
        } else if (active && !hasNvgHelmet(player)) {
            setActive(false)
        }
    }

    private fun setActive(value: Boolean) {
        active = value

        if (value) {
            (mc.gameRenderer as GameRendererInvoker).callLoadEffect(Mod.loc("shaders/post/nvg.json"))
            mc.player?.playSound(ModSounds.NIGHT_VISION_ACTIVATE.get(), 1f, 1f)
        } else if (mc.gameRenderer.currentEffect() != null) {
            // ponytail: post-эффект в ваниле один на всех, поэтому гасим текущий целиком.
            // Если тепловизор и ПНВ начнут включать одновременно — понадобится общий стек эффектов.
            mc.gameRenderer.shutdownEffect()
        }
    }
}
