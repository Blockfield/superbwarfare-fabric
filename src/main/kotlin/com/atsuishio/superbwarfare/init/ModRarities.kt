package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Style
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity

object ModRarities {
    @JvmField
    val LEGENDARY = ModEnumExtensions.legendary

    @JvmField
    val SUPERB: Rarity = ModEnumExtensions.superb

    @JvmField
    val VIRTUAL: Rarity = ModEnumExtensions.virtual

    private val LEGENDARY_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.GOLD)
    private val SUPERB_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.RED)
    private val VIRTUAL_STYLE: Style = Style.EMPTY.withColor(0xFF9AAF)

    /**
     * Все три модовые редкости схлопнуты в [Rarity.EPIC] (см. [ModEnumExtensions]), поэтому по
     * значению редкости их уже не отличить -- цвет имени возвращается по списку предметов.
     * Читает список [com.atsuishio.superbwarfare.mixins.ItemStackHoverNameMixin].
     *
     * ponytail: список руками. Условие пересмотра -- если модовых предметов этих трёх редкостей
     * станет заметно больше или они начнут появляться из данных: тогда либо расширять енум
     * (Fabric-ASM, entrypoint mm:early_risers), либо вешать редкость на свой data-компонент.
     */
    private val LEGENDARY_ITEMS =
        setOf(
            "aa_12",
            "javelin",
            "minigun",
            "ntw_20",
            "beast",
            "heavy_armament_module",
            "legendary_material_pack",
            "legendary_blueprint_data_chip",
            "aa_12_blueprint",
            "ntw_20_blueprint",
            "minigun_blueprint",
            "javelin_blueprint",
            "mk_42_blueprint",
            "mle_1934_blueprint",
            "bl_132_blueprint",
            "hpj_11_blueprint",
            "annihilator_blueprint",
        )

    private val SUPERB_ITEMS =
        setOf(
            "super_star_shooter",
            "superb_material_pack",
            "superb_blueprint_data_chip",
            "super_star_shooter_blueprint",
        )

    private val VIRTUAL_ITEMS =
        setOf(
            "trachelium",
            "secondary_cataclysm",
            "ql_1031",
            "virtual_material_pack",
            "virtual_blueprint_data_chip",
            "trachelium_blueprint",
            "secondary_cataclysm_blueprint",
            "ql_1031_blueprint",
        )

    private val styles: Map<Item, Style> by lazy {
        val map = HashMap<Item, Style>()

        fun put(
            ids: Set<String>,
            style: Style,
        ) {
            for (id in ids) {
                val item: Item? = BuiltInRegistries.ITEM.get(Mod.loc(id))
                if (item == null || item === Items.AIR) {
                    Mod.LOGGER.warn("ModRarities: предмет {} не найден, цвет редкости не применён", id)
                } else {
                    map[item] = style
                }
            }
        }
        put(LEGENDARY_ITEMS, LEGENDARY_STYLE)
        put(SUPERB_ITEMS, SUPERB_STYLE)
        put(VIRTUAL_ITEMS, VIRTUAL_STYLE)
        map
    }

    @JvmStatic
    fun styleOf(item: Item): Style? = styles[item]
}
