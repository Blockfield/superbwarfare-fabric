package com.atsuishio.superbwarfare.client.decorator

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

/**
 * Замена NeoForge-овскому IItemDecorator: сигнатура та же, но интерфейс свой, потому что в
 * Fabric API нет аналога RegisterItemDecorationsEvent. Декораторы лежат в
 * ClientRenderHandler.itemDecorators, зовёт их GuiGraphicsMixin.
 */
interface ItemDecorator {
    fun render(
        guiGraphics: GuiGraphics,
        font: Font,
        stack: ItemStack,
        xOffset: Int,
        yOffset: Int,
    ): Boolean
}
