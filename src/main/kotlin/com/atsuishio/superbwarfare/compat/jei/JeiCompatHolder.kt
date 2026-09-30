package com.atsuishio.superbwarfare.compat.jei

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.item.ItemStack

object JeiCompatHolder {
    const val JEI: String = "jei"

    @JvmStatic
    fun hasJEI(): Boolean = FabricLoader.getInstance().isModLoaded(JEI)

    @JvmStatic
    fun showRecipes(stack: ItemStack): Boolean = SbwJEIPlugin.showRecipes(stack)
}
