package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.item.StackAttributeItem;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * NeoForge звал IItemExtension#getDefaultAttributeModifiers(ItemStack) и позволял предмету считать
 * модификаторы по конкретному стаку. Ваниль читает только компонент ATTRIBUTE_MODIFIERS, поэтому
 * подмена делается здесь -- в forEachModifier, единственной точке, через которую LivingEntity
 * применяет атрибуты предмета и через которую строится подсказка.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAttributeMixin {

    @Shadow
    public abstract Item getItem();

    @Inject(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void sbw$perStackModifiers(
            EquipmentSlotGroup slotGroup,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci) {
        if (this.getItem() instanceof StackAttributeItem item) {
            ItemStack stack = (ItemStack) (Object) this;
            item.getDefaultAttributeModifiers(stack).forEach(slotGroup, consumer);
            EnchantmentHelper.forEachModifier(stack, slotGroup, consumer);
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void sbw$perStackModifiers(
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci) {
        if (this.getItem() instanceof StackAttributeItem item) {
            ItemStack stack = (ItemStack) (Object) this;
            item.getDefaultAttributeModifiers(stack).forEach(slot, consumer);
            EnchantmentHelper.forEachModifier(stack, slot, consumer);
            ci.cancel();
        }
    }
}
