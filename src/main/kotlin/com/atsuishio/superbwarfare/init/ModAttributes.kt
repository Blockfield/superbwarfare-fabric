package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute

/**
 * Общая сторона: вызывать из ModInitializer.
 *
 * Здесь не DeferredHolder, а сам Holder реестра: AttributeSupplier хранит атрибуты в HashMap по
 * Holder, а Holder.Reference сравнивается по ссылке. Обёртка DeferredHolder дала бы ключ, который
 * не находится ни командой /attribute, ни любым другим кодом, берущим Holder из реестра.
 *
 * BULLET_RESISTANCE навешивается на все живые типы миксином в LivingEntity.createLivingAttributes
 * (у NeoForge это делал EntityAttributeModificationEvent).
 */
object ModAttributes {
    @JvmField
    val BULLET_RESISTANCE: Holder<Attribute> =
        Registry.registerForHolder(
            BuiltInRegistries.ATTRIBUTE,
            ResourceLocation.fromNamespaceAndPath(Mod.MODID, "bullet_resistance"),
            RangedAttribute("attribute." + Mod.MODID + ".bullet_resistance", 0.0, 0.0, 1.0).setSyncable(true),
        )

    fun init() = Unit
}
