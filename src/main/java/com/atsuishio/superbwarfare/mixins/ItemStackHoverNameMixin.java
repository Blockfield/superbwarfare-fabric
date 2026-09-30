package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.init.ModRarities;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Цвета трёх модовых редкостей (legendary/superb/virtual). Расширить енум Rarity на Fabric нечем,
 * поэтому все три схлопнуты в EPIC, а цвет имени возвращается здесь по списку из ModRarities.
 *
 * <p>getHoverName -- единственный корень: через него красятся и тултип, и имя в хотбаре, и
 * getDisplayName (чат, смерть, рамки, JEI). Оборачиваем так же, как ванильные вызывающие: стиль
 * ставится на родителя, поэтому явный цвет самого имени (переименованный предмет) по-прежнему
 * перебивает наш.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHoverNameMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void superbwarfare$rarityColor(CallbackInfoReturnable<Component> cir) {
        Style style = ModRarities.styleOf(((ItemStack) (Object) this).getItem());
        if (style == null) return;

        Component name = cir.getReturnValue();
        if (name.getStyle().getColor() != null) return;

        cir.setReturnValue(Component.empty().append(name).withStyle(style));
    }
}
