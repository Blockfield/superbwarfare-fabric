package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.mixin.DamageAccess
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.ModifyResult
import com.atsuishio.superbwarfare.tools.DamageHandler.doDamage
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

fun Entity?.forceHurt(
    source: DamageSource,
    damage: Float,
): Boolean =
    if (this == null) {
        false
    } else {
        doDamage(this, source, damage)
    }

object DamageHandler {
    /**
     * Наносит урон, игнорируя кадры неуязвимости, если включён FORCE_DAMAGE_MODE.
     *
     * На NeoForge здесь лежала копия всего LivingEntity#hurt поверх DamageContainer.
     * Ни того, ни другого на Fabric нет, а копия расходится с ванилью на каждом апдейте,
     * поэтому сбрасываем ровно те два поля, из-за которых hurt() отказал, и повторяем вызов.
     */
    @JvmStatic
    fun doDamage(
        entity: Entity,
        source: DamageSource,
        damage: Float,
    ): Boolean {
        if (entity.hurt(source, damage)) return true
        if (entity !is LivingEntity || entity.level().isClientSide) return false
        if (!MiscConfig.FORCE_DAMAGE_MODE.get()) return false

        val access = DamageAccess.of(entity)
        val invulnerableTime = entity.invulnerableTime
        val lastHurt = access.`superbWarfare$getLastHurt`()

        entity.invulnerableTime = 0
        access.`superbWarfare$setLastHurt`(0f)
        if (entity.hurt(source, damage)) return true

        // Отказ был не из-за кадров неуязвимости: возвращаем состояние как было.
        entity.invulnerableTime = invulnerableTime
        access.`superbWarfare$setLastHurt`(lastHurt)
        return false
    }

    fun getDamageInfo(
        vehicle: VehicleEntity,
        source: DamageSource,
        amount: Float,
    ): MutableComponent {
        val detailedDamageResult = vehicle.getDamageModifier().matchResult(vehicle, source, amount)
        val finalDamage =
            if (detailedDamageResult.isEmpty()) amount else detailedDamageResult[detailedDamageResult.size - 1].damage

        val details =
            Component
                .empty()
                .append(
                    Component
                        .translatable(
                            "des.superbwarfare.vehicle_damage_analyzer.info.raw",
                            format2D(amount.toDouble()) + "\n",
                        ).withStyle(ChatFormatting.YELLOW)
                        .withStyle(ChatFormatting.UNDERLINE),
                ).append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(integrateInfo(detailedDamageResult))
                .append(
                    Component
                        .translatable(
                            "des.superbwarfare.vehicle_damage_analyzer.info.final",
                            format2D(finalDamage.toDouble()),
                        ).withStyle(ChatFormatting.GREEN),
                )

        return Component
            .literal("[")
            .append(vehicle.displayName ?: Component.empty())
            .append(Component.literal("] ").withStyle(ChatFormatting.WHITE))
            .append(
                Component
                    .translatable(
                        "des.superbwarfare.vehicle_damage_analyzer.info.raw",
                        format2D(amount.toDouble()),
                    ).withStyle(ChatFormatting.YELLOW),
            ).append(Component.literal(" => ").withStyle(ChatFormatting.WHITE))
            .append(
                Component
                    .translatable(
                        "des.superbwarfare.vehicle_damage_analyzer.info.final",
                        format2D(finalDamage.toDouble()),
                    ).withStyle(ChatFormatting.GREEN),
            ).withStyle {
                it.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        details,
                    ),
                )
            }
    }

    private fun integrateInfo(results: MutableList<ModifyResult>): MutableComponent {
        var info = Component.empty()
        for (result in results) {
            info = info.append(result.getDamageInfo()).append(Component.literal("\n").withStyle(ChatFormatting.RESET))
        }
        return info
    }
}
