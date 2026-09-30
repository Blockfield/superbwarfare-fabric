package com.atsuishio.superbwarfare.client.renderer.curio

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel
import com.atsuishio.superbwarfare.item.curio.ParachuteItem
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import io.wispforest.accessories.api.client.AccessoryRenderer
import io.wispforest.accessories.api.slot.SlotReference
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.CameraType
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ParachuteRenderer : AccessoryRenderer {
    private val model: ParachuteModel = ParachuteModel(mc.entityModels.bakeLayer(ParachuteModel.LAYER_LOCATION))

    override fun <M : LivingEntity> render(
        stack: ItemStack,
        reference: SlotReference,
        matrixStack: PoseStack,
        entityModel: EntityModel<M>,
        renderTypeBuffer: MultiBufferSource,
        light: Int,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        matrixStack.pushPose()

        matrixStack.scale(0.5f, 0.5f, 0.5f)
        matrixStack.translate(0.0, 1.25, 0.0)

        if (stack.getOrCreateTag().getBoolean(ParachuteItem.TAG_OPEN)) {
            val entity = reference.entity()
            this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks)
            this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)

            val vertexConsumer =
                ItemRenderer.getArmorFoilBuffer(
                    renderTypeBuffer,
                    RenderType.armorCutoutNoCull(TEXTURE),
                    stack.hasFoil(),
                )

            model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -0x1)
        }

        matrixStack.popPose()
    }

    companion object {
        private var parachuteModel: ParachuteModel? = null
        private val TEXTURE = loc("textures/curio/parachute.png")

        fun init() {
            WorldRenderEvents.AFTER_TRANSLUCENT.register { onRenderLevelStage(it) }
        }

        private fun onRenderLevelStage(context: WorldRenderContext) {
            val buffers = mc.renderBuffers()
            val player = localPlayer ?: return
            if (!ParachuteItem.isParachuteOpen(player)) return
            if (!ParachuteItem.isParachuteVisible(player)) return
            val stack = context.matrixStack() ?: return

            if (mc.options.cameraType == CameraType.FIRST_PERSON) {
                stack.pushPose()

                if (parachuteModel == null) {
                    parachuteModel =
                        ParachuteModel(
                            mc.entityModels.bakeLayer(ParachuteModel.LAYER_LOCATION),
                        )
                }

                stack.mulPose(Axis.XP.rotationDegrees(180f))
                stack.mulPose(Axis.YP.rotationDegrees(player.getViewYRot(1f)))
                stack.translate(0.0, 1.5, 0.0)

                parachuteModel!!.prepareMobModel(
                    player,
                    0f,
                    0f,
                    context.tickCounter().getGameTimeDeltaPartialTick(true),
                )
                parachuteModel!!.setupAnim(player, 0f, 0f, player.tickCount.toFloat(), 0f, 0f)
                parachuteModel!!.renderToBuffer(
                    stack,
                    buffers.bufferSource().getBuffer(
                        RenderType.armorCutoutNoCull(
                            TEXTURE,
                        ),
                    ),
                    0xFFFFFF,
                    OverlayTexture.NO_OVERLAY,
                    -0x1,
                )

                stack.popPose()
            }
        }

        /** RenderLivingEvent.Post: зовётся из LivingEntityRendererMixin после рендера сущности. */
        @JvmStatic
        fun onRenderLiving(
            entity: LivingEntity,
            stack: PoseStack,
            partialTick: Float,
        ) {
            if (entity is Player) return
            if (!ParachuteItem.isParachuteOpen(entity)) return
            if (!ParachuteItem.isParachuteVisible(entity)) return

            stack.pushPose()

            if (parachuteModel == null) {
                parachuteModel =
                    ParachuteModel(
                        mc.entityModels.bakeLayer(ParachuteModel.LAYER_LOCATION),
                    )
            }

            val buffers = mc.renderBuffers()

            stack.scale(0.5f, 0.5f, 0.5f)
            stack.mulPose(Axis.XP.rotationDegrees(180f))
            stack.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(1f)))
            stack.translate(0.0, -1.5, 0.0)

            parachuteModel!!.prepareMobModel(entity, 0f, 0f, partialTick)
            parachuteModel!!.setupAnim(entity, 0f, 0f, entity.tickCount.toFloat(), 0f, 0f)
            parachuteModel!!.renderToBuffer(
                stack,
                buffers.bufferSource().getBuffer(
                    RenderType.armorCutoutNoCull(
                        TEXTURE,
                    ),
                ),
                0xFFFFFF,
                OverlayTexture.NO_OVERLAY,
                -0x1,
            )

            stack.popPose()
        }
    }
}
