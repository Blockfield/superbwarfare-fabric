package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.item.HandGrenade
import com.atsuishio.superbwarfare.resource.model.ItemModelReloadListener
import com.atsuishio.superbwarfare.tools.deltaFrameTime
import com.atsuishio.superbwarfare.tools.mc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class HandGrenadeRenderer(
    dispatcher: BlockEntityRenderDispatcher,
    set: EntityModelSet,
) : BlockEntityWithoutLevelRenderer(dispatcher, set) {
    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        if (stack.item !is HandGrenade) return
        val instance = modelInstance ?: return
        poseStack.pushPose()

        // Процедурная поза бега: у гранаты нет GunGeoItem/gunRootMove, статичный рендер
        // иначе застывает на месте при спринте от первого лица.
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
            displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
        ) {
            applySprintPose(poseStack)
        }

        poseStack.translate(0.5f, 0.5f, 0.5f)

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityCutout(TEXTURE),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE),
            packedLight,
            OverlayTexture.NO_OVERLAY,
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/item/hand_grenade.png")
        val MODEL = loc("models/bedrock/item/hand_grenade.geo.json")
        val modelInstance = ItemModelReloadListener.getModel(MODEL)?.createInstance()

        private var sprintPose = 0f
        private var bobTime = 0.0

        private fun applySprintPose(poseStack: PoseStack) {
            val player =
                mc.player ?: run {
                    sprintPose = 0f
                    return
                }
            val times = mc.deltaFrameTime.coerceIn(0f, 0.8f)
            val active =
                player.isSprinting && player.onGround() &&
                    player.deltaMovement.horizontalDistance() > 0.08

            sprintPose =
                if (active) {
                    Mth.lerp(0.3f * times, sprintPose, 1f)
                } else {
                    Mth.lerp(1.4f * times, sprintPose, 0f)
                }
            if (sprintPose < 0.005f) {
                sprintPose = 0f
                return
            }

            if (active) bobTime += 0.15 * 2.0 * times * player.deltaMovement.horizontalDistance()
            val p = sprintPose
            val bobY = Mth.sin((bobTime % 1.0 * Math.PI).toFloat()) * 0.03f

            poseStack.translate(0.06f * p, (-0.16f * p + bobY.toFloat() * p), -0.1f * p)
            poseStack.mulPose(
                com.mojang.math.Axis.XP
                    .rotationDegrees(24f * p),
            )
            poseStack.mulPose(
                com.mojang.math.Axis.YP
                    .rotationDegrees(-18f * p),
            )
            poseStack.mulPose(
                com.mojang.math.Axis.ZP
                    .rotationDegrees(-10f * p),
            )
        }
    }
}
