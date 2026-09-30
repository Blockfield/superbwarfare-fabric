package com.atsuishio.superbwarfare.mixins;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Поля, которые NeoForge открывал технике: техника рассаживает пассажиров по местам сама и сама
 * выставляет задержку повторной посадки при высадке.
 */
@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("passengers")
    void sbw$setPassengers(ImmutableList<Entity> passengers);

    @Accessor("boardingCooldown")
    void sbw$setBoardingCooldown(int cooldown);
}
