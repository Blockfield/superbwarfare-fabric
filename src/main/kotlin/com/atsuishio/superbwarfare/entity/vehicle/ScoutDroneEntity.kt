package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

/**
 * Разведывательный дрон: летает и смотрит, но ничего не несёт.
 *
 * Отличия от [DroneEntity]: подвесы запрещены, кнопка сброса/подрыва ничего не делает,
 * дальность связи 200 блоков против 150 у FPV (предупреждение оператору со 150).
 * Прочность задаётся данными (`sbw/vehicles/scout_drone.json`).
 */
class ScoutDroneEntity(
    type: EntityType<out DroneEntity>,
    world: Level,
) : DroneEntity(type, world) {
    override val maxControlDistance: Double get() = 200.0

    override val weakSignalDistance: Double get() = 150.0

    override fun droneItem(): Item = ModItems.SCOUT_DRONE.get()

    // Не камикадзе и не бомбер: сообщение о выстреле игнорируем.
    override var fire: Boolean
        get() = false
        set(_) {}

    override fun interact(
        player: Player,
        hand: InteractionHand,
    ): InteractionResult {
        val stack = player.mainHandItem
        // Всё, кроме монитора и разборки шифтом, отбрасываем -- иначе базовый interact
        // навесил бы боеприпас из sbw/drone_attachments.
        if (!stack.isEmpty && !stack.`is`(ModItems.MONITOR.get()) && !player.isShiftKeyDown) {
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }
        return super.interact(player, hand)
    }
}
