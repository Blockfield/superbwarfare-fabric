package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.api.event.ExplosionEvent;
import com.atsuishio.superbwarfare.api.event.ExplosionKnockbackEvent;
import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess;
import com.atsuishio.superbwarfare.fabric.ModEventBus;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Implements ExplosionAccess to expose the explosion radius field to both client and server code
 * without reflection.
 *
 * <p>Также рассылает ExplosionEvent.Start/Detonate и ExplosionKnockbackEvent для ванильных взрывов
 * (замена патчей NeoForge). CustomExplosion переопределяет explode() без super и шлёт их сам.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionAccess {

    @Shadow @Final private float radius;

    @Shadow @Final private Level level;

    @Unique
    @Override
    public float superbwarfare$getRadius() {
        return this.radius;
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void sbw$explosionStart(CallbackInfo ci) {
        if (ModEventBus.INSTANCE
                .post(new ExplosionEvent.Start(this.level, (Explosion) (Object) this))
                .isCanceled()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "explode",
            at =
                    @At(
                            value = "INVOKE_ASSIGN",
                            target =
                                    "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> sbw$explosionDetonate(List<Entity> affectedEntities) {
        return ModEventBus.INSTANCE
                .post(
                        new ExplosionEvent.Detonate(
                                this.level, (Explosion) (Object) this, affectedEntities))
                .getAffectedEntities();
    }

    // Третий new Vec3 в explode(): vec31 -- вектор отброса; первые два -- позиция для gameEvent и
    // центр взрыва.
    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 2))
    private Vec3 sbw$explosionKnockback(Vec3 knockback, @Local(ordinal = 0) Entity entity) {
        return ModEventBus.INSTANCE
                .post(
                        new ExplosionKnockbackEvent(
                                this.level, (Explosion) (Object) this, entity, knockback))
                .getKnockbackVelocity();
    }
}
