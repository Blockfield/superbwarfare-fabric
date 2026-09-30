package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.fabric.findFirstEquipped
import com.atsuishio.superbwarfare.fabric.isAnotherEquipped
import com.atsuishio.superbwarfare.fabric.isSlotVisible
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.NBTTool
import io.wispforest.accessories.api.Accessory
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

class ParachuteItem :
    Item(Properties().stacksTo(1).durability(600)),
    Accessory {
    override fun isValidRepairItem(
        pStack: ItemStack,
        pRepairCandidate: ItemStack,
    ): Boolean = pRepairCandidate.`is`(Items.PHANTOM_MEMBRANE)

    override fun canEquip(
        stack: ItemStack,
        reference: SlotReference,
    ): Boolean = !isAnotherEquipped(stack, reference, this)

    override fun tick(
        stack: ItemStack,
        reference: SlotReference,
    ) {
        val entity = reference.entity()
        val tag = NBTTool.getTag(stack)
        if (entity !is Player) {
            if (!tag.getBoolean(TAG_OPEN) && entity.deltaMovement.y < -0.6 && entity.fallDistance > 4) {
                tag.putBoolean(TAG_OPEN, true)
                entity.level().playSound(
                    null,
                    entity.x,
                    entity.y,
                    entity.z,
                    ModSounds.PARACHUTE_OPEN.get(),
                    SoundSource.PLAYERS,
                    1f,
                    1f,
                )
            }
        }

        if (tag.getBoolean(TAG_OPEN)) {
            val level = entity.level()
            if ((entity.onGround() || entity.isInWater) || entity.isFallFlying || entity.vehicle != null ||
                (entity is Player && entity.abilities.flying)
            ) {
                tag.putBoolean(TAG_OPEN, false)
                NBTTool.saveTag(stack, tag)
                level.playSound(
                    null,
                    entity.x,
                    entity.y,
                    entity.z,
                    ModSounds.PARACHUTE_CLOSE.get(),
                    SoundSource.PLAYERS,
                    1f,
                    1f,
                )
            }
            if (entity is Player) {
                if (entity.level().isClientSide) {
                    entity.addDeltaMovement(
                        Vec3(entity.lookAngle.x, 0.0, entity.lookAngle.z).normalize().scale(0.05),
                    )
                    entity.deltaMovement = entity.deltaMovement.multiply(1.03, 0.75, 1.03)
                }
            } else {
                if (!entity.level().isClientSide) {
                    entity.addDeltaMovement(
                        Vec3(entity.lookAngle.x, 0.0, entity.lookAngle.z).normalize().scale(0.05),
                    )
                    entity.deltaMovement = entity.deltaMovement.multiply(1.03, 0.75, 1.03)
                }
            }

            if (entity.tickCount % 40 == 0 && level is ServerLevel) {
                stack.hurtAndBreak(1, level, entity as? ServerPlayer) { }
            }
            entity.resetFallDistance()
        }
    }

    companion object {
        const val TAG_OPEN: String = "Open"

        @JvmStatic
        fun isParachuteOpen(entity: LivingEntity?): Boolean {
            val equipped = findFirstEquipped(entity, ModItems.PARACHUTE.get()) ?: return false
            return NBTTool.getTag(equipped.stack()).getBoolean(TAG_OPEN)
        }

        fun isParachuteVisible(entity: LivingEntity?): Boolean {
            val equipped = findFirstEquipped(entity, ModItems.PARACHUTE.get()) ?: return false
            return equipped.reference().isSlotVisible
        }
    }
}
