package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.fabric.DeferredHolder
import com.atsuishio.superbwarfare.fabric.DeferredRegister
import com.atsuishio.superbwarfare.inventory.menu.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import java.util.function.Supplier

object ModMenuTypes {
    @JvmField
    val REGISTRY: DeferredRegister<MenuType<*>> = DeferredRegister.create(BuiltInRegistries.MENU, Mod.MODID)

    /**
     * Меню без дополнительных данных при открытии: у NeoForge это был
     * IMenuTypeExtension.create(IContainerFactory), где третий аргумент (буфер) не читался.
     *
     * Конструктор MenuType и интерфейс MenuType.MenuSupplier в ванили package-private, публичными
     * их делает транзитивный access widener из fabric-screen-handler-api-v1 (он же нужен и для
     * MenuScreens.register в ModScreens). Отдельный AW проекту заводить не нужно.
     */
    private fun <T : AbstractContainerMenu> register(
        name: String,
        factory: (Int, Inventory) -> T,
    ): DeferredHolder<MenuType<*>, MenuType<T>> =
        REGISTRY.register(
            name,
            Supplier<MenuType<T>> {
                MenuType(MenuType.MenuSupplier<T> { id, inv -> factory(id, inv) }, FeatureFlags.VANILLA_SET)
            },
        )

    /** Меню, у которых тип уже собран рядом с самим меню (техника -- ExtendedScreenHandlerType). */
    private fun <T : AbstractContainerMenu> registerExisting(
        name: String,
        type: MenuType<T>,
    ): DeferredHolder<MenuType<*>, MenuType<T>> = REGISTRY.register(name, Supplier<MenuType<T>> { type })

    @JvmField
    val REFORGING_TABLE_MENU =
        register("reforging_table_menu") { windowId, inv -> ReforgingTableMenu(windowId, inv) }

    @JvmField
    val CHARGING_STATION_MENU =
        register("charging_station_menu") { windowId, inv -> ChargingStationMenu(windowId, inv) }

    @JvmField
    val MINI_VEHICLE_CONTAINER_MENU = registerExisting("mini_vehicle_container", MiniVehicleContainerMenu.TYPE)

    @JvmField
    val SMALL_VEHICLE_CONTAINER_MENU = registerExisting("small_vehicle_container", SmallVehicleContainerMenu.TYPE)

    @JvmField
    val MEDIUM_VEHICLE_CONTAINER_MENU = registerExisting("medium_vehicle_container", MediumVehicleContainerMenu.TYPE)

    @JvmField
    val LARGE_VEHICLE_CONTAINER_MENU = registerExisting("large_vehicle_container", LargeVehicleContainerMenu.TYPE)

    @JvmField
    val HUGE_VEHICLE_CONTAINER_MENU = registerExisting("huge_vehicle_container", HugeVehicleContainerMenu.TYPE)

    @JvmField
    val SUPERB_ITEM_INTERFACE_MENU =
        register("superb_item_interface_menu") { windowId, inv -> SuperbItemInterfaceMenu(windowId, inv) }

    @JvmField
    val FUMO_25_MENU = register("fumo_25_menu") { windowId, inv -> FuMO25Menu(windowId, inv) }

    @JvmField
    val VEHICLE_ASSEMBLING_MENU =
        register("vehicle_assembling_menu") { windowId, inv -> VehicleAssemblingMenu(windowId, inv) }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE =
        register("blueprint_research_table_menu") { windowId, inv -> BlueprintResearchTableMenu(windowId, inv) }
}
