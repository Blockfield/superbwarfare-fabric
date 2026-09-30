package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Прицел и хотбар в технике и при взятом стволе. У NeoForge это делал RenderGuiLayerEvent.Pre;
 * HudRenderCallback у Fabric рисует поверх HUD и отменить отдельный ванильный слой не умеет,
 * поэтому слой гасится прямо здесь.
 */
@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class GuiHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void sbw$hideCrosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ClientEventHandler.shouldHideCrossHair()) ci.cancel();
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void sbw$hideHotbar(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ClientEventHandler.shouldHideHotbar()) ci.cancel();
    }
}
