package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.fabric.isAnotherEquipped
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import io.wispforest.accessories.api.Accessory
import io.wispforest.accessories.api.slot.SlotReference
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import java.util.*

class DogTagItem :
    Item(Properties().stacksTo(1)),
    Accessory,
    ItemScreenProvider,
    IVehicleInteract {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        TooltipTool.addScreenProviderText(tooltipComponents)
    }

    override fun canEquip(
        stack: ItemStack,
        reference: SlotReference,
    ): Boolean = !isAnotherEquipped(stack, reference, this)

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> = Optional.of(DogTagImageComponent(pStack))

    @Environment(EnvType.CLIENT)
    override fun getItemScreen(
        stack: ItemStack,
        player: Player,
        hand: InteractionHand,
    ): Screen = DogTagEditorScreen(stack, hand)

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand,
    ): InteractionResult? {
        if (!player.isShiftKeyDown) return null
        vehicle.dogTagIcon = getColors(stack).map { it.toList() }.toList()
        return InteractionResult.SUCCESS
    }

    companion object {
        @JvmStatic
        fun getColors(stack: ItemStack): Array<ShortArray> {
            val colors: Array<ShortArray> = Array(16) { ShortArray(16) }
            for (el in colors) {
                Arrays.fill(el, (-1).toShort())
            }

            val data = stack.get(ModDataComponents.DOG_TAG_IMAGE.get()).takeIf { !it.isNullOrEmpty() } ?: return colors

            // intersect, а не union: индекс за пределами более короткого массива -- это падение,
            // а не пустой цвет. Компонент приходит с клиента (DogTagFinishEditMessage).
            for (i in data.indices intersect colors.indices) {
                val color = data[i].takeIf { it.isNotEmpty() } ?: continue
                for (j in color.indices intersect colors[i].indices) {
                    colors[i][j] = color[j]
                }
            }

            return colors
        }
    }
}
