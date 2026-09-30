package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature

/**
 * Замена data/superbwarfare/neoforge/biome_modifier: у Fabric биом-модификаторы -- код,
 * а не данные. Ключи размещённых фич остаются в data/superbwarfare/worldgen/placed_feature.
 */
object ModBiomeModifications {
    private val ORES =
        listOf(
            "galena_ore",
            "deepslate_galena_ore",
            "scheelite_ore",
            "deepslate_scheelite_ore",
            "silver_ore",
            "deepslate_silver_ore",
        )

    fun init() {
        val overworld = BiomeSelectors.foundInOverworld()

        ORES.forEach {
            BiomeModifications.addFeature(
                overworld,
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Mod.loc(it)),
            )
        }

        BiomeModifications.addSpawn(overworld, MobCategory.MONSTER, ModEntities.SENPAI.get(), 20, 4, 4)
        BiomeModifications.addSpawn(overworld, MobCategory.MONSTER, ModEntities.STEEL_COIL.get(), 5, 1, 3)
    }
}
