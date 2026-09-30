package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.StackAttributeItem
import com.atsuishio.superbwarfare.resource.model.ArmorModelReloadListener
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.GeoArmorRendererV2
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import kotlin.math.max

/**
 * Поножи «6Б»: защита 6 берётся из материала, а прочность 600 и вязкость 3.0 — как в старом
 * fracturepoint (WARBORN_ARMOR: множитель 40 при базе LEGGINGS 15, toughness 3.0). Вязкость
 * материала (4.0) поэтому заменяется своим модификатором.
 */
class RuLeggingsItem :
    ArmorItem(
        ModArmorMaterial.CEMENTED_CARBIDE,
        Type.LEGGINGS,
        Properties().durability(600),
    ),
    StackAttributeItem {
    companion object {
        const val TOUGHNESS = 3.0

        val TOUGHNESS_ID = loc("leggings_toughness")
        val TEXTURE = loc("textures/bedrock/armor/ru_leggings.png")
        val MODEL = loc("models/bedrock/armor/ru_leggings.geo.json")

        /** Клиент: аналога IClientItemExtensions#getHumanoidArmorModel на Fabric нет, модель брони отдаётся через ArmorRenderer. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: GeoArmorRendererV2? = null

            ArmorRenderer.register({ poseStack, buffer, stack, entity, slot, light, contextModel ->
                if (renderer == null) {
                    renderer =
                        GeoArmorRendererV2(
                            ArmorModelReloadListener.getModel(MODEL),
                            slot,
                            TEXTURE,
                        )
                }

                renderer!!.preparePose(entity, stack, slot, contextModel)
                renderer!!.renderArmorToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f)
            }, ModItems.RU_LEGGINGS.get())
        }
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val slotGroup = EquipmentSlotGroup.bySlot(this.type.slot)
        val list =
            ArrayList<ItemAttributeModifiers.Entry>(
                baseAttributeModifiers(stack).modifiers().filter { it.attribute() != Attributes.ARMOR_TOUGHNESS },
            )
        list.add(
            ItemAttributeModifiers.Entry(
                Attributes.ARMOR_TOUGHNESS,
                AttributeModifier(TOUGHNESS_ID, TOUGHNESS, AttributeModifier.Operation.ADD_VALUE),
                slotGroup,
            ),
        )
        list.add(
            ItemAttributeModifiers.Entry(
                ModAttributes.BULLET_RESISTANCE,
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.1 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                slotGroup,
            ),
        )
        return ItemAttributeModifiers(list, true)
    }
}
