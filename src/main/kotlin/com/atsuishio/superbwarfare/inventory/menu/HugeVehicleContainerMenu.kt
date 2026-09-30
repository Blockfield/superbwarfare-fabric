package com.atsuishio.superbwarfare.inventory.menu

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class HugeVehicleContainerMenu(
    id: Int,
    inventory: Inventory,
    entityId: Int,
) : AbstractVehicleContainerMenu(TYPE, id, inventory, entityId) {
    override fun getRows(): Int = 6

    override fun addVehicleInventory() {
        for (r in 0 until getRows()) {
            for (c in 0 until 17) {
                this.addSlot(VehicleSlot(this.vehicle, c + r * 17, 8 + c * 18 - 72, 18 + r * 18))
            }
        }
    }

    companion object {
        // id сущности-техники едет на клиент через данные открытия меню:
        // фабричный аналог IMenuTypeExtension.create(IContainerFactory).
        // Сервер обязан отдавать его из ExtendedScreenHandlerFactory<Int>.getScreenOpeningData.
        @JvmField
        val TYPE: MenuType<HugeVehicleContainerMenu> =
            ExtendedScreenHandlerType<HugeVehicleContainerMenu, Int>(
                { id, inventory, entityId -> HugeVehicleContainerMenu(id, inventory, entityId) },
                ByteBufCodecs.VAR_INT,
            )
    }
}
