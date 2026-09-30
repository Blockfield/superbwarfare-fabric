package com.atsuishio.superbwarfare.mixins;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SpriteContents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * NeoForge отдаёт исходную картинку спрайта через SpriteContents.getOriginalImage(), в ваниле поле
 * приватное. Нужно, чтобы брать цвет пикселя блока под колёсами (SpritePixelHelper).
 */
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
    @Accessor("originalImage")
    NativeImage getOriginalImage();
}
