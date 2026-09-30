package com.atsuishio.superbwarfare.client.renderer.curio

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.client.model.curio.ThermalImagingGogglesModel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import io.wispforest.accessories.api.client.AccessoryRenderer
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

class ThermalImagingGogglesRenderer : AccessoryRenderer {
    private val model: ThermalImagingGogglesModel =
        ThermalImagingGogglesModel(
            mc.entityModels.bakeLayer(ThermalImagingGogglesModel.LAYER_LOCATION),
        )

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
        val entity = reference.entity()
        this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks)
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
        followHeadRotations(entity, this.model.bone)
        val vertexConsumer =
            ItemRenderer.getArmorFoilBuffer(
                renderTypeBuffer,
                RenderType.armorCutoutNoCull(TEXTURE),
                false,
            )
        model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -1)
        matrixStack.popPose()
    }

    companion object {
        private val TEXTURE = loc("textures/curio/thermal_imaging_goggles.png")

        /**
         * Замена `ICurioRenderer.followHeadRotations`: у AccessoryRenderer аналога нет,
         * поэтому копируем поворот головы гуманоидной модели сущности в части модели аксессуара.
         */
        private fun followHeadRotations(
            entity: LivingEntity,
            vararg parts: ModelPart,
        ) {
            val renderer = mc.entityRenderDispatcher.getRenderer(entity)
            val model = (renderer as? LivingEntityRenderer<*, *>)?.model as? HumanoidModel<*> ?: return
            for (part in parts) part.copyFrom(model.head)
        }
    }
}
