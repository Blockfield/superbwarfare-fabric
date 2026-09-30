package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType

/**
 * Общая сторона: вызывать из ModInitializer.
 *
 * У апстрима это был глобальный модификатор лута: реестр GLOBAL_LOOT_MODIFIER_SERIALIZERS плюс
 * четыре JSON-описания в data/superbwarfare/loot_modifiers. На Fabric такого реестра нет, есть
 * LootTableEvents.MODIFY, поэтому пары «ванильная таблица -> таблица мода» переехали в код,
 * а сама подмешиваемая таблица подключается пулом с записью-ссылкой.
 *
 * ponytail: список пар зашит в исходник. Если понадобится дать его паквизу/датапакам --
 * читать те же JSON через ResourceManager.
 */
object ModLootModifier {
    private fun vanilla(path: String) = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/$path"))

    private fun mod(path: String) = ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("chests/$path"))

    private val COMMON = mod("blue_print_common")
    private val RARE = mod("blue_print_rare")
    private val EPIC = mod("blue_print_epic")
    private val ANCIENT_CPU = mod("ancient_cpu")

    private val EXTRA_LOOT: List<Pair<ResourceKey<LootTable>, ResourceKey<LootTable>>> =
        listOf(
            vanilla("simple_dungeon") to COMMON,
            vanilla("abandoned_mineshaft") to COMMON,
            vanilla("shipwreck_map") to COMMON,
            vanilla("shipwreck_supply") to COMMON,
            vanilla("shipwreck_treasure") to COMMON,
            vanilla("ruined_portal") to COMMON,
            vanilla("ancient_city") to RARE,
            vanilla("ancient_city_ice_box") to RARE,
            vanilla("bastion_bridge") to RARE,
            vanilla("bastion_hoglin_stable") to RARE,
            vanilla("bastion_other") to RARE,
            vanilla("buried_treasure") to RARE,
            vanilla("desert_pyramid") to RARE,
            vanilla("igloo") to RARE,
            vanilla("jungle_temple") to RARE,
            vanilla("pillager_outpost") to EPIC,
            vanilla("stronghold_library") to EPIC,
            vanilla("woodland_mansion") to EPIC,
            vanilla("end_city_treasure") to EPIC,
            vanilla("ancient_city") to ANCIENT_CPU,
        )

    /** Тот же геймрул, что проверял TargetModLootTableModifier.doApply у апстрима. */
    private val DO_GENERATE_LOOTS_TYPE: LootItemConditionType =
        Registry.register(
            BuiltInRegistries.LOOT_CONDITION_TYPE,
            Mod.loc("do_generate_loots"),
            LootItemConditionType(MapCodec.unit(DoGenerateLoots)),
        )

    private object DoGenerateLoots : LootItemCondition {
        override fun getType(): LootItemConditionType = DO_GENERATE_LOOTS_TYPE

        override fun test(context: LootContext): Boolean = context.level.gameRules.getBoolean(ModGameRules.MOD_RULE_DO_GENERATE_LOOTS)
    }

    fun init() {
        LootTableEvents.MODIFY.register { key, tableBuilder, _, _ ->
            EXTRA_LOOT.forEach { (target, extra) ->
                if (target == key) {
                    tableBuilder.withPool(
                        LootPool
                            .lootPool()
                            .add(NestedLootTable.lootTableReference(extra))
                            .`when` { DoGenerateLoots },
                    )
                }
            }
        }
    }
}
