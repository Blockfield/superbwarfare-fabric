package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.MilitaryShovelRenderer
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.mc
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.ChatFormatting
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.*
import net.minecraft.world.item.component.Tool
import net.minecraft.world.item.context.UseOnContext

open class MilitaryShovelItem :
    AxeItem(
        ModItemTier.CEMENTED_CARBIDE,
        CustomDamageProperty(810)
            .rarity(Rarity.RARE)
            .component(
                DataComponents.TOOL,
                Tool(
                    listOf(
                        Tool.Rule.deniesDrops(ModItemTier.CEMENTED_CARBIDE.incorrectBlocksForDrops),
                        Tool.Rule.minesAndDrops(
                            ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL,
                            ModItemTier.CEMENTED_CARBIDE.speed,
                        ),
                    ),
                    1f,
                    1,
                ),
            ).attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, 2f, -2.6f)),
    ) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.military_shovel").withStyle(ChatFormatting.GRAY),
        )
    }

    /**
     * Апстрим повторял здесь логику Mekanism-Tools поверх NeoForge-овских ItemAbilities: лопата
     * умеет всё, что умеют топор, лопата и мотыга. На Fabric ItemAbility нет, зато ванильные
     * useOn у AxeItem/ShovelItem/HoeItem делают ровно эти три набора действий и сами бьют по
     * прочности того стака, что лежит в UseOnContext, -- то есть по нашей лопате.
     *
     * По действиям с блоками ничего не потеряно и терять нечего: AXE_STRIP/AXE_SCRAPE/
     * AXE_WAX_OFF/SHOVEL_FLATTEN/SHOVEL_DOUSE/HOE_TILL -- это и есть ванильные useOn, а модовые
     * блоки на Fabric регистрируются в те же ванильные таблицы (StrippableBlockRegistry,
     * TillableBlockRegistry, FlattenableBlockRegistry из Fabric API), которые эти useOn читают.
     * Своя таблица превращений сделала бы хуже: она бы про чужие блоки не знала.
     *
     * ponytail: единственная реальная потеря -- ItemAbilities.SWORD_SWEEP: в ванили 1.21.1
     * размашистая атака включается по `instanceof SwordItem` в Player.attack, а лопата -- AxeItem.
     * Тегами блоков это не лечится, нужен миксин в Player.attack (@ModifyExpressionValue на
     * INSTANCEOF). Возвращаться, если ближний бой лопатой станет важнее, чем один хрупкий
     * инжектор в горячем методе.
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val axe = super.useOn(context)
        if (axe != InteractionResult.PASS) return axe

        val vanilla = if (context.player?.isShiftKeyDown == true) Items.IRON_HOE else Items.IRON_SHOVEL
        return vanilla.useOn(context)
    }

    override fun getEnchantmentValue(): Int = ModItemTier.CEMENTED_CARBIDE.enchantmentValue

    companion object {
        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: BlockEntityWithoutLevelRenderer? = null

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.MILITARY_SHOVEL.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    if (renderer == null) {
                        renderer = MilitaryShovelRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    renderer!!.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }
    }
}
