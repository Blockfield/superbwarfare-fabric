package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tools.NBTTool
import io.github.fabricators_of_create.porting_lib.entity.events.player.PlayerEvents
import io.github.fabricators_of_create.porting_lib.item.extensions.DamageableItem
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.TooltipFlag
import org.joml.Math

open class HammerItem(
    tier: Tier,
    attackDamage: Int,
    attackSpeed: Float,
    properties: Properties,
) : SwordItem(tier, properties.attributes(createAttributes(tier, attackDamage, attackSpeed))),
    DamageableItem {
    constructor(tier: Tier, attackDamage: Int, attackSpeed: Float, maxDamage: Int) : this(
        tier,
        attackDamage,
        attackSpeed,
        CustomDamageProperty(maxDamage),
    )

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        TooltipTool.addHideText(
            tooltipComponents,
            Component
                .translatable("des.superbwarfare.hammer", NBTTool.getTag(stack).getInt("CraftCount"))
                .withStyle(ChatFormatting.GRAY),
        )
    }

    // NeoForge звал hasCraftingRemainingItem(stack) и getCraftingRemainingItem(stack) парой;
    // на Fabric это один getRecipeRemainder(stack) из FabricItem, а has... всегда возвращал true.
    override fun getRecipeRemainder(itemstack: ItemStack): ItemStack {
        val stack = itemstack.copy()

        val tag = NBTTool.getTag(stack)
        tag.putInt("CraftCount", tag.getInt("CraftCount") + 1)
        NBTTool.saveTag(stack, tag)

        if (!itemstack.isDamageableItem) return stack

        stack.damageValue = itemstack.damageValue + 1

        if (stack.damageValue >= stack.maxDamage) {
            return ItemStack.EMPTY
        }
        return stack
    }

    override fun isRepairable(itemstack: ItemStack): Boolean = true

    override fun hurtEnemy(
        stack: ItemStack,
        target: LivingEntity,
        attacker: LivingEntity,
    ): Boolean {
        attacker.level().playSound(
            null,
            target.onPos,
            ModSounds.MELEE_HIT.get(),
            SoundSource.PLAYERS,
            1f,
            ((2 * Math.random() - 1) * 0.1f + 1.0f).toFloat(),
        )
        return super.hurtEnemy(stack, target, attacker)
    }

    companion object {
        /** Общий. */
        fun init() {
            PlayerEvents.ItemCraftedEvent.EVENT.register { onItemCraftedByHammer(it) }
        }

        private fun onItemCraftedByHammer(event: PlayerEvents.ItemCraftedEvent) {
            val item = event.crafting
            val container = event.inventory
            val player = event.entity

            if (player.level().isClientSide) return

            if (item.`is`(ModTags.Items.HAMMER)) {
                var count = 0
                for (i in 0..<container.containerSize) {
                    if (container.getItem(i).`is`(ModTags.Items.HAMMER)) count++
                }
                if (count == 2) {
                    container.clearContent()
                }
            }
        }
    }
}
