package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.item.DamageFilterItem;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Замена IItemExtension#canBeHurtBy из NeoForge: контейнеры с техникой внутри не должны
 * уничтожаться взрывом или кактусом.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityHurtMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void sbw$canBeHurtBy(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof DamageFilterItem item && !item.canBeHurtBy(stack, source)) {
            cir.setReturnValue(false);
        }
    }
}
