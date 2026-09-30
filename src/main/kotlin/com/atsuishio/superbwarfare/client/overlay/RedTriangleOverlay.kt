package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

@Environment(EnvType.CLIENT)
object RedTriangleOverlay : CommonOverlay("red_triangle") {
    private val TRIANGLE = loc("textures/overlay/rpg/red_triangle.png")

    override fun shouldRender(): Boolean {
        if (MiscConfig.HIDE_COMBAT_HUD.get()) return false
        return super.shouldRender()
    }

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        val stack = player.mainHandItem
        if (stack.`is`(ModItems.RPG.get()) && from(stack).selectedAmmoType.get() == 0) {
            // Маркер захвата -- про технику: обе наши ракеты РПГ неуправляемые, и треугольник над
            // игроком/мобом/мишенью только вводил в заблуждение. Ищем именно технику, а не ближайшее
            // живое существо, иначе игрок перед танком забирал бы захват себе.
            val idf =
                SeekTool
                    .seekQuery(player) {
                        withinRange(128.0)
                        withinAngle(6.0)
                        baseFilter()
                        smokeFilter()
                        notFriendly()
                        isNotOwner()
                        noClip()
                        custom(java.util.function.Predicate { it is VehicleEntity })
                    }.buildWithClosest() ?: return

            val distance = idf.position().distanceTo(cameraPos)
            val pos =
                Vec3(
                    Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(), idf.xo, idf.x),
                    Mth.lerp(
                        deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(),
                        idf.yo + idf.eyeHeight + 0.5 + 0.07 * distance,
                        idf.eyeY + 0.5 + 0.07 * distance,
                    ),
                    Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(), idf.zo, idf.z),
                )
            val point = pos.worldToScreen()

            poseStack.pushPose()
            val x = point.x.toFloat()
            val y = point.y.toFloat()

            RenderHelper.preciseBlit(guiGraphics, TRIANGLE, x - 4, y - 4, 8f, 8f, 0f, 0f, 8f, 8f, 8f, 8f)

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            poseStack.popPose()
        }
    }
}
