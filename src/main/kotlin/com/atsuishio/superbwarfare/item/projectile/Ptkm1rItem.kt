package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Ptkm1rItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem
import com.atsuishio.superbwarfare.tools.mc
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level

open class Ptkm1rItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE).stacksTo(2)) {
    companion object {
        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: BlockEntityWithoutLevelRenderer? = null

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.PTKM_1R.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    if (renderer == null) {
                        renderer = Ptkm1rItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    renderer!!.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }
    }

    override fun spawnDeployedEntity(
        level: Level,
        player: Player,
    ): Entity = Ptkm1rEntity(player, level)
}
