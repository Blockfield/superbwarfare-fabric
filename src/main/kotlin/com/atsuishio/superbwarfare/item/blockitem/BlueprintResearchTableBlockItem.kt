package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.client.renderer.item.BlueprintResearchingTableBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.mc
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.item.BlockItem
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class BlueprintResearchTableBlockItem :
    BlockItem(ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(), Properties()),
    GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    companion object {
        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: BlockEntityWithoutLevelRenderer? = null

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.BLUEPRINT_RESEARCH_TABLE.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    if (renderer == null) {
                        renderer =
                            BlueprintResearchingTableBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    renderer!!.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }
    }
}
