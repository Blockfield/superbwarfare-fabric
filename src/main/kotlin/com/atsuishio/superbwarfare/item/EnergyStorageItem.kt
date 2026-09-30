package com.atsuishio.superbwarfare.item

import net.minecraft.world.item.ItemStack

interface EnergyStorageItem {
    fun getMaxEnergy(stack: ItemStack): Int

    fun getMaxReceiveEnergy(stack: ItemStack): Int = getMaxEnergy(stack)

    fun getMaxExtractEnergy(stack: ItemStack): Int = getMaxEnergy(stack)
}
