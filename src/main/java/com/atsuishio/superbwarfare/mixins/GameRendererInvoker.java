package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * NeoForge держит GameRenderer.loadEffect публичным, в ваниле он приватный. Тепловизор и очки
 * грузят свой post-шейдер только через него.
 */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("loadEffect")
    void callLoadEffect(ResourceLocation resourceLocation);
}
