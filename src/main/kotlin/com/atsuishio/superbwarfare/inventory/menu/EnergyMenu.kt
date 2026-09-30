package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot.Companion.forContainer
import com.atsuishio.superbwarfare.network.message.receive.ContainerDataMessage
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuCloseMessage
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuOpenMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.google.common.collect.Lists
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.MenuType

abstract class EnergyMenu(
    pMenuType: MenuType<*>?,
    id: Int,
    containerData: ContainerEnergyData,
) : AbstractContainerMenu(pMenuType, id) {
    private val containerEnergyDataSlots: MutableList<ContainerEnergyDataSlot> = Lists.newArrayList()
    private val usingPlayers: MutableList<ServerPlayer> = ArrayList()

    init {
        for (i in 0..<containerData.getCount()) {
            addDataSlot(DataSlot.standalone())
            this.containerEnergyDataSlots.add(forContainer(containerData, i))
        }
    }

    override fun broadcastChanges() {
        val pairs: MutableList<ContainerDataMessage.Pair> = ArrayList()
        for (i in this.containerEnergyDataSlots.indices) {
            val dataSlot = this.containerEnergyDataSlots[i]
            if (dataSlot.checkAndClearUpdateFlag()) pairs.add(ContainerDataMessage.Pair(i, dataSlot.get()))
        }

        if (!pairs.isEmpty()) {
            this.usingPlayers.forEach { p ->
                p.sendPacket(ContainerDataMessage(this.containerId, pairs))
            }
        }

        super.broadcastChanges()
    }

    override fun setData(
        id: Int,
        data: Int,
    ) {
        super.setData(id, data)
        if (id < 0 || id >= this.containerEnergyDataSlots.size) {
            Mod.LOGGER.error("EnergyMenu.setData id out of bounds: {}", id)
            return
        }
        this.containerEnergyDataSlots[id].set(data.toLong())
    }

    /**
     * Аналога PlayerContainerEvent.Open/Close на Fabric нет: ни Fabric API, ни Porting Lib
     * не публикуют открытие и закрытие контейнера. Логика сохранена как приватные функции,
     * остаётся вызвать их из миксина на Player#openMenu и Player#closeContainer.
     */
    companion object {
        private fun onContainerOpened(
            menu: AbstractContainerMenu,
            player: Player,
        ) {
            if (menu is EnergyMenu && player is ServerPlayer) {
                menu.usingPlayers.add(player)

                val toSync: MutableList<ContainerDataMessage.Pair> = ArrayList()
                for (i in menu.containerEnergyDataSlots.indices) {
                    toSync.add(ContainerDataMessage.Pair(i, menu.containerEnergyDataSlots[i].get()))
                }
                player.sendPacket(ContainerDataMessage(menu.containerId, toSync))
            }
        }

        private fun onContainerClosed(
            menu: AbstractContainerMenu,
            player: Player,
        ) {
            if (menu is EnergyMenu && player is ServerPlayer) {
                menu.usingPlayers.remove(player)
            }
        }

        private fun onFuMO25Opened(
            menu: AbstractContainerMenu,
            player: Player,
        ) {
            if (menu is FuMO25Menu && player is ServerPlayer) {
                menu.selfPos.ifPresent { pos ->
                    player.sendPacket(RadarMenuOpenMessage(pos))
                }
            }
        }

        private fun onFuMO25Closed(
            menu: AbstractContainerMenu,
            player: Player,
        ) {
            if (menu is FuMO25Menu && player is ServerPlayer) {
                player.sendPacket(RadarMenuCloseMessage)
            }
        }
    }
}
