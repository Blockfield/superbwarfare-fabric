package com.atsuishio.superbwarfare.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * NeoForge делал {@link Level#getEntities()} публичным; на Fabric метод остаётся protected, а
 * EntityFindUtil ходит в него и для ServerLevel, и для ClientLevel.
 */
@Mixin(Level.class)
public interface LevelEntitiesAccessor {

    @Invoker("getEntities")
    LevelEntityGetter<Entity> sbw$callGetEntities();
}
