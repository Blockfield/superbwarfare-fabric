package com.atsuishio.superbwarfare.inventory.menu

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class LargeVehicleContainerMenu(
    id: Int,
    inventory: Inventory,
    entityId: Int,
) : AbstractVehicleContainerMenu(TYPE, id, inventory, entityId) {
    override fun getRows(): Int = 6

    override fun addVehicleInventory() {
        for (r in 0 until getRows()) {
            for (c in 0 until 13) {
                this.addSlot(VehicleSlot(this.vehicle, c + r * 13, 8 + c * 18 - 36, 18 + r * 18))
            }
        }
    }

    companion object {
        // id сущности-техники едет на клиент через данные открытия меню:
        // фабричный аналог IMenuTypeExtension.create(IContainerFactory).
        // Сервер обязан отдавать его из ExtendedScreenHandlerFactory<Int>.getScreenOpeningData.
        @JvmField
        val TYPE: MenuType<LargeVehicleContainerMenu> =
            ExtendedScreenHandlerType<LargeVehicleContainerMenu, Int>(
                { id, inventory, entityId -> LargeVehicleContainerMenu(id, inventory, entityId) },
                ByteBufCodecs.VAR_INT,
            )
    }
}
