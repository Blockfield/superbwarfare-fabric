package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.config.server.MapConfig
import com.atsuishio.superbwarfare.fabric.isAccessoryEquipped
import com.atsuishio.superbwarfare.fabric.isAnotherEquipped
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.network.message.receive.OpenTacticalMapScreenMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import io.wispforest.accessories.api.Accessory
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

open class TacticalTerminalItem :
    Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)),
    Accessory {
    override fun canEquip(
        stack: ItemStack,
        reference: SlotReference,
    ): Boolean = !isAnotherEquipped(stack, reference, this)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        TooltipTool.addDevelopingText(tooltip)
        if (!MapConfig.ENABLE_TACTICAL_MAP.get()) {
            tooltip.add(
                Component.translatable("des.superbwarfare.tactical_terminal.disabled").withStyle(ChatFormatting.RED),
            )
        }
        tooltip.add(
            Component
                .translatable(
                    "des.superbwarfare.tactical_terminal",
                    Component
                        .literal("[${ModKeyMappings.TOGGLE_TACTICAL_MAP.translatedKeyMessage.string}]")
                        .withStyle(ChatFormatting.AQUA),
                ).withStyle(ChatFormatting.GRAY),
        )
    }

    override fun use(
        pLevel: Level,
        pPlayer: Player,
        pUsedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = pPlayer.getItemInHand(pUsedHand)
        if (!MapConfig.ENABLE_TACTICAL_MAP.get()) {
            return InteractionResultHolder.fail(stack)
        }

        val level = pPlayer.level()
        if (!level.isClientSide) {
            pPlayer.sendPacket(OpenTacticalMapScreenMessage)
        }
        return InteractionResultHolder.consume(stack)
    }

    companion object {
        @JvmStatic
        fun isTerminalEquipped(entity: LivingEntity?): Boolean = isAccessoryEquipped(entity, ModItems.TACTICAL_TERMINAL.get())
    }
}
