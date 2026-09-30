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
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import kotlin.math.max

class RuHelmet6b47Item :
    ArmorItem(
        ModArmorMaterial.CEMENTED_CARBIDE,
        Type.HELMET,
        Properties().durability(Type.HELMET.getDurability(50)),
    ),
    StackAttributeItem {
    companion object {
        val TEXTURE = loc("textures/bedrock/armor/ru_helmet_6b47.png")
        val MODEL = loc("models/bedrock/armor/ru_helmet_6b47.geo.json")

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
            }, ModItems.RU_HELMET_6B47.get())
        }
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val modifiers = baseAttributeModifiers(stack)
        val list = ArrayList<ItemAttributeModifiers.Entry>(modifiers.modifiers())
        list.add(
            ItemAttributeModifiers.Entry(
                ModAttributes.BULLET_RESISTANCE,
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.2 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.bySlot(this.type.slot),
            ),
        )
        return ItemAttributeModifiers(list, true)
    }
}
