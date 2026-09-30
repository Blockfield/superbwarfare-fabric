package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.init.ModAttachments
import com.atsuishio.superbwarfare.init.getData
import com.mojang.serialization.Codec
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

class PhosphorusFireCapability {
    var isOnFire: Boolean = false

    fun writeToNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putBoolean(TAG_PHOSPHORUS_FIRE, this.isOnFire)
        return tag
    }

    fun readFromNBT(nbt: CompoundTag) {
        if (nbt.contains(TAG_PHOSPHORUS_FIRE)) {
            this.isOnFire = nbt.getBoolean(TAG_PHOSPHORUS_FIRE)
        }
    }

    companion object {
        val ID: ResourceLocation = loc("phosphorus_fire_capability")
        const val TAG_PHOSPHORUS_FIRE: String = "SbwPhosphorusFire"

        /** Хранится тем же тегом, что и на NeoForge, — чтобы старые миры читались без миграции. */
        @JvmField
        val CODEC: Codec<PhosphorusFireCapability> =
            CompoundTag.CODEC.xmap(
                { tag -> PhosphorusFireCapability().apply { readFromNBT(tag) } },
                PhosphorusFireCapability::writeToNBT,
            )

        @JvmStatic
        fun of(living: LivingEntity): PhosphorusFireCapability = living.getData(ModAttachments.PHOSPHORUS_FIRE)
    }
}
