package com.atsuishio.superbwarfare.fabric

import io.github.fabricators_of_create.porting_lib.entity.PartEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.EnderDragonPart

/**
 * Замена PartEntity из NeoForge.
 *
 * SBW не объявляет ни одной составной сущности — он только обрабатывает чужие: попали в часть,
 * перенаправили на владельца. Источников таких частей на Fabric два, и они не пересекаются:
 * ванильные части Эндер-дракона и моды, написанные на PartEntity из Porting Lib. Проверять
 * нужно оба, поэтому обращение сведено сюда.
 *
 * ponytail: мод со своей системой составных сущностей мимо Porting Lib сюда не попадёт —
 * добавлять его тип здесь, все шесть мест ходят через этот помощник.
 */
object MultipartEntities {
    fun isPart(entity: Entity): Boolean = entity is EnderDragonPart || entity is PartEntity<*>

    fun parentOf(entity: Entity): Entity? =
        when (entity) {
            is EnderDragonPart -> entity.parentMob
            is PartEntity<*> -> entity.parent
            else -> null
        }
}
