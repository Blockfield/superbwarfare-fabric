package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.PlayerEventHandler;

import kotlin.Triple;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Замена AnvilUpdateEvent из NeoForge: ствол + Shortcut Pack повышает уровень оружия.
 * Только @Inject: lrtactical тоже инжектится в HEAD createResult, каждый отменяет лишь свои пары.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow @Final private DataSlot cost;

    @Shadow private int repairItemCountCost;

    public AnvilMenuMixin(
            MenuType<?> type,
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void sbw$anvilUpdate(CallbackInfo ci) {
        Triple<ItemStack, Integer, Integer> result =
                PlayerEventHandler.onAnvilUpdate(
                        this.inputSlots.getItem(0), this.inputSlots.getItem(1));
        if (result == null) return;
        this.resultSlots.setItem(0, result.getFirst());
        this.cost.set(result.getSecond());
        this.repairItemCountCost = result.getThird();
        ci.cancel();
    }
}
