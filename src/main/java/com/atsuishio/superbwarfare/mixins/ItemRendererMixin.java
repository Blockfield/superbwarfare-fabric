package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.fabric.SeparateTransformsBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Вторая половина `neoforge:separate_transforms` (первая -- в NeoForgeModelLoaders): если у
 * перспективы задана своя модель, рендерим её вместо обёртки. Рекурсии нет -- модель перспективы
 * обёрткой уже не является.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void superbwarfare$separateTransforms(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci) {
        if (!(model instanceof SeparateTransformsBakedModel separate)) return;

        BakedModel perspective = separate.forPerspective(displayContext);
        if (perspective == model) return;

        ci.cancel();
        ((ItemRenderer) (Object) this)
                .render(
                        stack,
                        displayContext,
                        leftHand,
                        poseStack,
                        buffer,
                        light,
                        overlay,
                        perspective);
    }
}
