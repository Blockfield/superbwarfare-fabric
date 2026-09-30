package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.StackAttributeItem
import com.atsuishio.superbwarfare.resource.model.ArmorModelReloadListener
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonArmorHandler
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.GeoArmorRendererV2
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import kotlin.math.max

class RuChest6b43Item :
    ArmorItem(
        ModArmorMaterial.CEMENTED_CARBIDE,
        Type.CHESTPLATE,
        Properties().durability(Type.CHESTPLATE.getDurability(50)),
    ),
    StackAttributeItem {
    companion object {
        val SHOULDERPADS_ID = loc("shoulderpads")
        val TEXTURE = loc("textures/bedrock/armor/ru_chest_6b43.png")
        val MODEL = loc("models/bedrock/armor/ru_chest_6b43.geo.json")

        /** Клиент: аналога IClientItemExtensions#getHumanoidArmorModel на Fabric нет, модель брони отдаётся через ArmorRenderer. */
        @Environment(EnvType.CLIENT)
        fun init() {
            var renderer: GeoArmorRendererV2? = null

            fun getRenderer(slot: EquipmentSlot): GeoArmorRendererV2 {
                if (renderer == null) {
                    renderer =
                        GeoArmorRendererV2(
                            ArmorModelReloadListener.getModel(MODEL),
                            slot,
                            TEXTURE,
                        )
                }
                return renderer!!
            }

            ArmorRenderer.register({ poseStack, buffer, stack, entity, slot, light, contextModel ->
                val armorRenderer = getRenderer(slot)
                armorRenderer.preparePose(entity, stack, slot, contextModel)
                armorRenderer.renderArmorToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f)
            }, ModItems.RU_CHEST_6B43.get())

            FirstPersonArmorHandler.register(ModItems.RU_CHEST_6B43.get()) { getRenderer(EquipmentSlot.CHEST) }
        }
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val modifiers = baseAttributeModifiers(stack)
        val list = ArrayList<ItemAttributeModifiers.Entry>(modifiers.modifiers())
        // Наплечники: в fracturepoint это был отдельный Curios-предмет на +4 брони, здесь они
        // нарисованы прямо на жилете, поэтому +4 идут отдельным модификатором к защите 8.
        list.add(
            ItemAttributeModifiers.Entry(
                Attributes.ARMOR,
                AttributeModifier(SHOULDERPADS_ID, 4.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.bySlot(this.type.slot),
            ),
        )
        list.add(
            ItemAttributeModifiers.Entry(
                ModAttributes.BULLET_RESISTANCE,
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.5 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.bySlot(this.type.slot),
            ),
        )
        return ItemAttributeModifiers(list, true)
    }
}
