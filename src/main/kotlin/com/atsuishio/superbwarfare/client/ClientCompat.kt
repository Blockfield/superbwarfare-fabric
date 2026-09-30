package com.atsuishio.superbwarfare.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/**
 * NeoForge открывает KeyMapping.getKey(), из-за чего в апстриме работает `mapping.key`.
 * В ваниле поле приватное, и его отдаёт только KeyBindingHelper из fabric-key-binding-api-v1.
 */
val KeyMapping.boundKey: InputConstants.Key
    get() = KeyBindingHelper.getBoundKeyOf(this)

/**
 * NeoForge добавляет к GuiGraphics.drawString перегрузки с float-координатами, и весь HUD мода
 * считает позиции во float. В ваниле есть только int, поэтому дробную часть кладём в матрицу --
 * позиция символов остаётся ровно той же, что была на NeoForge.
 */
fun GuiGraphics.drawString(
    font: Font,
    text: String,
    x: Float,
    y: Float,
    color: Int,
    dropShadow: Boolean,
): Int = withTranslation(x, y) { drawString(font, text, 0, 0, color, dropShadow) }

fun GuiGraphics.drawString(
    font: Font,
    text: Component,
    x: Float,
    y: Float,
    color: Int,
    dropShadow: Boolean,
): Int = withTranslation(x, y) { drawString(font, text, 0, 0, color, dropShadow) }

fun GuiGraphics.drawString(
    font: Font,
    text: FormattedCharSequence,
    x: Float,
    y: Float,
    color: Int,
    dropShadow: Boolean,
): Int = withTranslation(x, y) { drawString(font, text, 0, 0, color, dropShadow) }

private inline fun GuiGraphics.withTranslation(
    x: Float,
    y: Float,
    block: GuiGraphics.() -> Int,
): Int {
    pose().pushPose()
    pose().translate(x, y, 0f)
    val result = block()
    pose().popPose()
    return result
}
