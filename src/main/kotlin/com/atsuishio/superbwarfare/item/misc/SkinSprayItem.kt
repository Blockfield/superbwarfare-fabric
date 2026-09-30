package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.client.renderer.item.SkinSprayRenderer
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.network.message.receive.OpenVehicleSkinScreenMessage
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class SkinSprayItem :
    Item(Properties().stacksTo(1)),
    IVehicleInteract {
    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand,
    ): InteractionResult {
        val level = player.level()
        if (!level.isClientSide) {
            player.sendPacket(OpenVehicleSkinScreenMessage(vehicle.id))
        }
        return InteractionResult.CONSUME
    }

    companion object {
        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: BlockEntityWithoutLevelRenderer? = null

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.SKIN_SPRAY.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    if (renderer == null) {
                        renderer = SkinSprayRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    renderer!!.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }
    }
}
