package com.atsuishio.superbwarfare.compat.create

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Method

/**
 * Рейкаст пуль по Create-контрапшенам (поезда): контрапшен-сущности isPickable=false и hurt=false,
 * поэтому без компата пули пролетают состав насквозь. Рефлексия — superbwarfare не имеет
 * зависимости от Create при сборке.
 */
object CreateProjectileCompat {
    data class ContraptionHit(
        val entity: Entity,
        val location: Vec3,
        val blockState: BlockState?,
    )

    private val contraptionClass: Class<*>?
    private val rayTraceMethod: Method?
    private val toGlobalMethod: Method?
    private val getContraptionMethod: Method?
    private val getBlocksMethod: Method?

    init {
        var cls: Class<*>? = null
        var rayTrace: Method? = null
        var toGlobal: Method? = null
        var getContraption: Method? = null
        var getBlocks: Method? = null
        if (FabricLoader.getInstance().isModLoaded("create")) {
            try {
                cls = Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity")
                val contraptionCls = Class.forName("com.simibubi.create.content.contraptions.Contraption")
                val handlerCls = Class.forName("com.simibubi.create.content.contraptions.ContraptionHandlerClient")
                rayTrace = handlerCls.getMethod("rayTraceContraption", Vec3::class.java, Vec3::class.java, cls)
                toGlobal = cls.getMethod("toGlobalVector", Vec3::class.java, Float::class.java)
                getContraption = cls.getMethod("getContraption")
                getBlocks = contraptionCls.getMethod("getBlocks")
            } catch (t: Throwable) {
                cls = null
            }
        }
        contraptionClass = cls
        rayTraceMethod = rayTrace
        toGlobalMethod = toGlobal
        getContraptionMethod = getContraption
        getBlocksMethod = getBlocks
    }

    /** Ближайший блок контрапшена на отрезке [start, end] в мировых координатах, либо null. */
    @JvmStatic
    fun rayTraceContraption(
        level: Level,
        start: Vec3,
        end: Vec3,
        owner: Entity?,
    ): ContraptionHit? {
        val cls = contraptionClass ?: return null
        val rayTrace = rayTraceMethod ?: return null
        if (level !is ServerLevel) return null

        val box = AABB(start, end).inflate(1.0)
        var best: ContraptionHit? = null
        var bestDist = Double.MAX_VALUE

        for (entity in level.getEntitiesOfClass(Entity::class.java, box)) {
            if (!cls.isInstance(entity)) continue
            if (owner != null && (entity == owner.vehicle || entity.rootVehicle === owner.rootVehicle)) continue

            val result =
                try {
                    rayTrace.invoke(null, start, end, entity) as? BlockHitResult ?: continue
                } catch (t: Throwable) {
                    continue
                }

            val global =
                try {
                    toGlobalMethod!!.invoke(entity, result.location, 1.0f) as? Vec3 ?: continue
                } catch (t: Throwable) {
                    continue
                }

            val dist = start.distanceToSqr(global)
            if (dist < bestDist) {
                bestDist = dist
                best = ContraptionHit(entity, global, blockStateOf(entity, result.blockPos))
            }
        }
        return best
    }

    private fun blockStateOf(
        entity: Entity,
        localPos: BlockPos,
    ): BlockState? {
        return try {
            val contraption = getContraptionMethod!!.invoke(entity)
            val blocks = getBlocksMethod!!.invoke(contraption) as? Map<*, *> ?: return null
            val info = blocks[localPos] ?: return null
            info.javaClass.getMethod("state").invoke(info) as? BlockState
        } catch (t: Throwable) {
            null
        }
    }
}
