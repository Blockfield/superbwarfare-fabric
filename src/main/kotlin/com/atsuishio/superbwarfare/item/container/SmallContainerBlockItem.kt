package com.atsuishio.superbwarfare.item.container

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.client.renderer.item.SmallContainerBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.DamageFilterItem
import com.atsuishio.superbwarfare.tools.mc
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.SeededContainerLoot
import net.minecraft.world.level.storage.loot.LootTable

class SmallContainerBlockItem :
    BlockItem(ModBlocks.SMALL_CONTAINER.get(), Properties().stacksTo(1).fireResistant()),
    DamageFilterItem {
    // На NeoForge это был IItemExtension#canBeHurtBy; огнестойкость ваниль проверяет сама,
    // поэтому от вызова super остались только иммунитеты мода.
    override fun canBeHurtBy(
        stack: ItemStack,
        source: DamageSource,
    ) = !source.`is`(DamageTypeTags.IS_EXPLOSION) && !source.`is`(DamageTypes.CACTUS)

    companion object {
        @JvmField
        val SMALL_CONTAINERS: MutableList<() -> ItemStack> =
            mutableListOf(
                { createInstance(loc("containers/blueprints")) },
                { createInstance(loc("containers/common")) },
            )

        @JvmOverloads
        fun createInstance(
            lootTable: ResourceLocation,
            lootTableSeed: Long = 0L,
        ): ItemStack = createInstance(ResourceKey.create(Registries.LOOT_TABLE, lootTable), lootTableSeed)

        @JvmOverloads
        fun createInstance(
            lootTable: ResourceKey<LootTable>,
            lootTableSeed: Long = 0L,
        ): ItemStack {
            val stack = ItemStack(ModBlocks.SMALL_CONTAINER.get())
            stack.set(
                DataComponents.CONTAINER_LOOT,
                SeededContainerLoot(lootTable, lootTableSeed),
            )
            return stack
        }

        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: BlockEntityWithoutLevelRenderer? = null

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.SMALL_CONTAINER.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    if (renderer == null) {
                        renderer = SmallContainerBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    renderer!!.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }
    }
}
