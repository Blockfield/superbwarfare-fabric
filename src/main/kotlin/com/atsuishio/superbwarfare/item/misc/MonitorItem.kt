package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.control.DroneControlAccess
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.StackAttributeItem
import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.sendPacket
import io.github.fabricators_of_create.porting_lib.item.extensions.ReequipAnimationItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import javax.annotation.ParametersAreNonnullByDefault

open class MonitorItem :
    Item(Properties().stacksTo(1)),
    StackAttributeItem,
    ReequipAnimationItem {
    @Environment(EnvType.CLIENT)
    private fun beginCamera() {
        ClientEventHandler.lastCameraType = Minecraft.getInstance().options.cameraType
        Minecraft.getInstance().options.cameraType = CameraType.THIRD_PERSON_BACK
    }

    @Environment(EnvType.CLIENT)
    private fun restoreCamera() {
        Minecraft.getInstance().options.cameraType =
            ClientEventHandler.lastCameraType ?: CameraType.FIRST_PERSON
    }

    @ParametersAreNonnullByDefault
    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand,
    ): InteractionResultHolder<ItemStack?> {
        // Control packets use the main hand; an off-hand use must not toggle the other stack.
        if (hand != InteractionHand.MAIN_HAND) return super.use(level, player, hand)
        val stack = player.mainHandItem
        val tag = NBTTool.getTag(stack)
        val drone = EntityFindUtil.findDrone(level, tag.getString(LINKED_DRONE))

        if (tag.getBoolean(USING)) {
            // Always permit leaving the view, including after the entity disappeared.
            tag.putBoolean(USING, false)
            NBTTool.saveTag(stack, tag)
            if (level.isClientSide) restoreCamera()
            if (player is ServerPlayer) player.sendPacket(ResetCameraTypeMessage)
            if (!level.isClientSide && drone != null && DroneControlAccess.owns(player, drone)) {
                DroneControlAccess.resetIfUncontrolled(drone)
            }
        } else {
            if (drone == null || !DroneControlAccess.canUse(player, drone, requireUsing = false)) {
                if (player is ServerPlayer) player.sendPacket(ResetCameraTypeMessage)
                return super.use(level, player, hand)
            }
            tag.putBoolean(USING, true)
            NBTTool.saveTag(stack, tag)
            if (level.isClientSide) {
                beginCamera()
            } else {
                // Clear any leftover session before minting the new one for this activation.
                DroneControlAccess.resetInput(drone)
                drone.beginControlSession()
            }
        }
        return super.use(level, player, hand)
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val list = ArrayList(baseAttributeModifiers(stack).modifiers())
        list.addAll(
            listOf(
                ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND,
                ),
                ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_SPEED,
                    AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND,
                ),
            ),
        )

        return ItemAttributeModifiers(list, true)
    }

    @Environment(EnvType.CLIENT)
    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component?>,
        tooltipFlag: TooltipFlag,
    ) {
        val tag = NBTTool.getTag(stack)
        if (!tag.contains(LINKED_DRONE) || tag.getString(LINKED_DRONE) == "none") return

        val player: Player? = Minecraft.getInstance().player
        if (player == null) return

        if (!tag.contains("PosX") || !tag.contains("PosY") || !tag.contains("PosZ")) return

        val droneVec = Vec3(tag.getDouble("PosX"), tag.getDouble("PosY"), tag.getDouble("PosZ"))

        tooltipComponents.add(
            Component
                .translatable(
                    "des.superbwarfare.monitor",
                    format1D(player.position().distanceTo(droneVec), "m"),
                ).withStyle(ChatFormatting.GRAY),
        )
        tooltipComponents.add(
            Component.literal(
                "X: " + format1D(droneVec.x) +
                    " Y: " + format1D(droneVec.y) +
                    " Z: " + format1D(droneVec.z),
            ),
        )
    }

    override fun shouldCauseReequipAnimation(
        oldStack: ItemStack,
        newStack: ItemStack,
        slotChanged: Boolean,
    ): Boolean = false

    @ParametersAreNonnullByDefault
    override fun inventoryTick(
        stack: ItemStack,
        world: Level,
        entity: Entity,
        slot: Int,
        selected: Boolean,
    ) {
        super.inventoryTick(stack, world, entity, slot, selected)
        val tag = NBTTool.getTag(stack)
        // Dormant monitor copies do not perform lookups or reset a different active monitor.
        if (!tag.getBoolean(USING)) return
        val player = entity as? Player
        val drone = EntityFindUtil.findDrone(world, tag.getString(LINKED_DRONE))
        if (selected && player != null && drone != null && DroneControlAccess.canUse(player, drone)) return

        tag.putBoolean(USING, false)
        NBTTool.saveTag(stack, tag)
        if (world.isClientSide && (player == null || DroneControlAccess.resolve(player) == null)) restoreCamera()
        if (player is ServerPlayer && selected) player.sendPacket(ResetCameraTypeMessage)
        if (!world.isClientSide && player != null && drone != null && DroneControlAccess.owns(player, drone)) {
            DroneControlAccess.resetIfUncontrolled(drone)
        }
    }

    companion object {
        const val LINKED: String = "Linked"
        const val LINKED_DRONE: String = "LinkedDrone"
        const val USING: String = "Using"

        @JvmStatic
        fun link(
            tag: CompoundTag,
            id: String,
        ) {
            tag.putBoolean(USING, false)
            tag.putBoolean(LINKED, true)
            tag.putString(LINKED_DRONE, id)
        }

        @JvmStatic
        fun disLink(
            tag: CompoundTag,
            player: Player?,
        ) {
            val wasUsing = tag.getBoolean(USING)
            val drone = player?.let { EntityFindUtil.findDrone(it.level(), tag.getString(LINKED_DRONE)) }
            tag.putBoolean(USING, false)
            tag.putBoolean(LINKED, false)
            tag.putString(LINKED_DRONE, "none")
            if (player != null && !player.level().isClientSide && drone != null && DroneControlAccess.owns(player, drone)) {
                DroneControlAccess.resetInput(drone)
            }
            // A dormant linked stack must not reset the view of a different active drone.
            if (wasUsing && player is ServerPlayer) player.sendPacket(ResetCameraTypeMessage)
        }

        @JvmStatic
        fun getDronePos(
            stack: ItemStack,
            vec3: Vec3,
        ) {
            val tag = NBTTool.getTag(stack)
            tag.putDouble("PosX", vec3.x)
            tag.putDouble("PosY", vec3.y)
            tag.putDouble("PosZ", vec3.z)
            NBTTool.saveTag(stack, tag)
        }
    }
}
