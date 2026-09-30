package com.atsuishio.superbwarfare.mixins;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code Mob.goalSelector} is protected; NeoForge widened it via access transformer. */
@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {

    @Accessor("goalSelector")
    GoalSelector sbw$goalSelector();
}
