package com.atsuishio.superbwarfare.compat

import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader

object CompatHolder {
    const val DMV: String = "dreamaticvoyage"
    const val VRC: String = "virtuarealcraft"
    const val CLOTH_CONFIG: String = "cloth-config" // id на Fabric через дефис, не через подчёркивание
    const val TACZ: String = "tacz"

    @JvmField
    val hasTacz: Boolean = FabricLoader.getInstance().isModLoaded(TACZ)

    fun init() {
        hasMod(CLOTH_CONFIG) {
            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                ClothConfigHelper.registerScreen()
            }
        }
    }

    fun hasMod(
        modid: String,
        runnable: Runnable,
    ) {
        if (FabricLoader.getInstance().isModLoaded(modid)) {
            runnable.run()
        }
    }
}
