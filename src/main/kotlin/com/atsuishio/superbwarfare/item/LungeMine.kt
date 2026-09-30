package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.renderer.item.LungeMineRenderer
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModEnumExtensions
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.localPlayer
import io.github.fabricators_of_create.porting_lib.item.extensions.EntitySwingListenerItem
import io.github.fabricators_of_create.porting_lib.item.extensions.ReequipAnimationItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil

// 不要改这个东西，会肘击 YSM
open class LungeMine :
    Item(Properties().stacksTo(4)),
    GeoItem,
    EntitySwingListenerItem,
    ReequipAnimationItem,
    StackAttributeItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    fun getTransformType(type: ItemDisplayContext?) {
        transformType = type
    }

    private fun idlePredicate(event: AnimationState<LungeMine?>): PlayState? {
        val player = localPlayer ?: return PlayState.STOP
        if (ClientEventHandler.lungeSprint > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.sprint"))
        }

        if (ClientEventHandler.lungeDraw > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.draw"))
        }

        if (ClientEventHandler.lungeAttack > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.fire"))
        }

        if (player.isSprinting && player.onGround() && ClientEventHandler.lungeDraw == 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lunge_mine.run"))
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lunge_mine.idle"))
    }

    override fun registerControllers(data: ControllerRegistrar) {
        val idleController =
            AnimationController<LungeMine>(
                this,
                "idleController",
                2,
            ) { this.idlePredicate(it) }
        data.add(idleController)
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache? = this.cache

    // Porting Lib отдаёт onEntitySwing без InteractionHand, руку тут всё равно не использовали.
    override fun onEntitySwing(
        stack: ItemStack,
        entity: LivingEntity,
    ): Boolean = false

    override fun shouldCauseReequipAnimation(
        oldStack: ItemStack,
        newStack: ItemStack,
        slotChanged: Boolean,
    ): Boolean = false

    override fun use(
        worldIn: Level,
        playerIn: Player,
        handIn: InteractionHand,
    ): InteractionResultHolder<ItemStack?> {
        val stack = playerIn.getItemInHand(handIn)
        if (playerIn is ServerPlayer) {
            playerIn
                .level()
                .playSound(null, playerIn.onPos, ModSounds.LUNGE_MINE_GROWL.get(), SoundSource.PLAYERS, 2f, 1f)
        }
        if (!playerIn.level().isClientSide()) {
            playerIn.addEffect(
                MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    100,
                    (
                        if (playerIn.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                            playerIn
                                .getEffect(MobEffects.MOVEMENT_SPEED)!!
                                .amplifier
                        } else {
                            0
                        }
                    ) + 2,
                ),
            )
        } else {
            ClientEventHandler.lungeSprint = 180
        }
        playerIn.cooldowns.addCooldown(stack.item, 300)
        return InteractionResultHolder.consume(stack)
    }

    override fun canAttackBlock(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean = false

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val list = ArrayList(baseAttributeModifiers(stack).modifiers())

        // 移速
        list.add(
            ItemAttributeModifiers.Entry(
                Attributes.ENTITY_INTERACTION_RANGE,
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    1.5,
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.MAINHAND,
            ),
        )

        return ItemAttributeModifiers(list, true)
    }

    companion object {
        var transformType: ItemDisplayContext? = null

        /** Клиент: BEWLR из IClientItemExtensions#getCustomRenderer заменён на DynamicItemRenderer из Fabric API. */
        @Environment(EnvType.CLIENT)
        fun init() {
            val renderer: BlockEntityWithoutLevelRenderer = LungeMineRenderer()

            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.LUNGE_MINE.get(),
                BuiltinItemRendererRegistry.DynamicItemRenderer { stack, mode, poseStack, buffer, light, overlay ->
                    renderer.renderByItem(stack, mode, poseStack, buffer, light, overlay)
                },
            )
        }

        // Аналога IClientItemExtensions#getArmPose на Fabric нет (ни в Fabric API, ни в Porting Lib):
        // осталась функцией без регистрации, звать из миксина на HumanoidModel/PlayerRenderer.
        @Environment(EnvType.CLIENT)
        fun getArmPose(
            entityLiving: LivingEntity,
            hand: InteractionHand,
            itemStack: ItemStack,
        ): ArmPose {
            if (!itemStack.isEmpty) {
                if (entityLiving.usedItemHand == hand) {
                    return ModEnumExtensions.Client.lungeMinePose
                }
            }
            return ArmPose.EMPTY
        }
    }
}
