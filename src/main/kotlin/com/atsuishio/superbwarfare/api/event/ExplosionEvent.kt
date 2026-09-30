package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.fabric.CancellableEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/**
 * Замена ExplosionEvent из NeoForge: рассылается через ModEventBus и для ванильных взрывов
 * (ExplosionMixin), и для [com.atsuishio.superbwarfare.tools.CustomExplosion].
 */
open class ExplosionEvent(
    val level: Level,
    val explosion: Explosion,
) {
    /** Перед расчётом взрыва; отменённый взрыв не ломает блоки и не задевает сущности. */
    class Start(
        val level: Level,
        val explosion: Explosion,
    ) : CancellableEvent()

    /** Список поражаемых сущностей уже собран, но урон ещё не нанесён -- список можно править. */
    class Detonate(
        level: Level,
        explosion: Explosion,
        val affectedEntities: MutableList<Entity>,
    ) : ExplosionEvent(level, explosion)
}

/** Замена ExplosionKnockbackEvent из NeoForge: [knockbackVelocity] можно подменить до применения. */
class ExplosionKnockbackEvent(
    level: Level,
    explosion: Explosion,
    val affectedEntity: Entity,
    var knockbackVelocity: Vec3,
) : ExplosionEvent(level, explosion)
