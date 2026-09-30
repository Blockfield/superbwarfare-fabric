package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.LivingEventHandler;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Замена ItemEntityPickupEvent.Pre из NeoForge: игрок в технике складывает подобранное в её
 * инвентарь. Условия те же, что у NeoForge перед событием -- задержка подбора вышла и предмет не
 * закреплён за другим игроком.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Shadow private int pickupDelay;

    @Shadow private UUID target;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void sbw$vehiclePickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide
                || this.pickupDelay != 0
                || (this.target != null && !this.target.equals(player.getUUID()))) {
            return;
        }
        if (LivingEventHandler.onPickup(player, self)) {
            ci.cancel();
        }
    }
}
