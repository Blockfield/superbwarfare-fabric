package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.item.LungeMine
import com.atsuishio.superbwarfare.item.gun.launcher.SuperStarShooterItem
import com.atsuishio.superbwarfare.item.gun.machinegun.M2HBItem
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Rarity

/**
 * Расширения ванильных енумов: у NeoForge это RegisterEnumExtensionsEvent + EnumProxy,
 * у Fabric штатного механизма нет. Обе константы по-прежнему схлопнуты в ванильные, но
 * наблюдаемое поведение возвращено миксинами:
 *
 * 1. Rarity. Три собственные редкости (legendary/GOLD, superb/RED, virtual/розовый) остались
 *    Rarity.EPIC -- сортировка, компоненты предмета и теги чертежей от редкости не зависят,
 *    рантайм её вообще не читает. Цвет имени возвращает
 *    [com.atsuishio.superbwarfare.mixins.ItemStackHoverNameMixin] по списку предметов
 *    в [ModRarities]; конструктор Rarity(int, String, UnaryOperator[Style]) из патча NeoForge
 *    (нужный для virtual) при этом не требуется.
 *
 * 2. ArmPose. Четыре собственные позы рук остались ArmPose.CROSSBOW_HOLD (значение нужно только
 *    для ещё не подключённого IClientItemExtensions#getArmPose у предметов), а сами углы
 *    из апстрима применяет [Client.applyCustomArmPose] из
 *    [com.atsuishio.superbwarfare.mixins.HumanoidModelMixin].
 *
 * ponytail: полноценное расширение енумов (Fabric-ASM, ClassTinkerers.enumBuilder в entrypoint
 * `mm:early_risers`) не сделано -- оно требует явной зависимости в build.gradle.kts и правки
 * общего fabric.mod.json, а всё видимое игроку и так закрыто двумя миксинами. Возвращаться
 * стоит, только если сторонний код начнёт сравнивать редкости/позы по значению енума.
 */
object ModEnumExtensions {
    @JvmField
    val legendary: Rarity = Rarity.EPIC

    @JvmField
    val superb: Rarity = Rarity.EPIC

    @JvmField
    val virtual: Rarity = Rarity.EPIC

    @Environment(EnvType.CLIENT)
    object Client {
        @JvmStatic
        val lungeMinePose: ArmPose = ArmPose.CROSSBOW_HOLD

        @JvmStatic
        val minigunPose: ArmPose = ArmPose.CROSSBOW_HOLD

        @JvmStatic
        val m2Pose: ArmPose = ArmPose.CROSSBOW_HOLD

        @JvmStatic
        val superStarShooterPose: ArmPose = ArmPose.CROSSBOW_HOLD

        /**
         * Замена IArmPoseTransformer: если в главной руке предмет с собственной позой, выставляет
         * углы обеих рук как в апстриме и просит миксин отменить ванильную позу.
         *
         * Апстримовые трансформеры работают только когда их зовут для правой руки (они сразу
         * ставят обе), поэтому для левой мы лишь отменяем ванильную обработку, чтобы
         * poseLeftArm не затёр выставленные углы.
         *
         * ponytail: поза берётся из главной руки и применяется к правой. Для игрока с
         * «главная рука -- левая» позы не будет, как и в апстриме на левой руке.
         */
        @JvmStatic
        fun applyCustomArmPose(
            model: HumanoidModel<*>,
            entity: LivingEntity,
            arm: HumanoidArm,
        ): Boolean {
            if (entity.usedItemHand != InteractionHand.MAIN_HAND) return false

            val item = entity.getItemInHand(InteractionHand.MAIN_HAND).item
            if (item !is LungeMine && item !is MinigunItem && item !is M2HBItem && item !is SuperStarShooterItem) {
                return false
            }

            if (arm == HumanoidArm.RIGHT) {
                when (item) {
                    is LungeMine -> lungeMineArms(model)
                    is MinigunItem -> minigunArms(model)
                    is M2HBItem -> m2Arms(model)
                    else -> superStarShooterArms(model)
                }
            }
            return true
        }

        private fun lungeMineArms(model: HumanoidModel<*>) {
            model.rightArm.xRot = 20f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = -12f * Mth.DEG_TO_RAD
            model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot
            model.leftArm.yRot = 40f * Mth.DEG_TO_RAD
        }

        private fun minigunArms(model: HumanoidModel<*>) {
            model.rightArm.xRot = 22.5f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = model.head.yRot
            model.leftArm.xRot =
                Mth.clamp(
                    -45f * Mth.DEG_TO_RAD + model.head.xRot,
                    -67.5f * Mth.DEG_TO_RAD,
                    0f,
                )
            model.leftArm.yRot =
                Mth.clamp(
                    45f * Mth.DEG_TO_RAD + model.head.yRot,
                    45f * Mth.DEG_TO_RAD,
                    80f * Mth.DEG_TO_RAD,
                )
        }

        private fun m2Arms(model: HumanoidModel<*>) {
            model.rightArm.xRot = 45f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = model.head.yRot
            model.leftArm.xRot =
                Mth.clamp(
                    -45f * Mth.DEG_TO_RAD + model.head.xRot,
                    -67.5f * Mth.DEG_TO_RAD,
                    0f,
                )
            model.leftArm.yRot =
                Mth.clamp(
                    45f * Mth.DEG_TO_RAD + model.head.yRot,
                    45f * Mth.DEG_TO_RAD,
                    80f * Mth.DEG_TO_RAD,
                )
        }

        private fun superStarShooterArms(model: HumanoidModel<*>) {
            model.rightArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = 0f
            model.rightArm.zRot = 0f
            model.leftArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
            model.leftArm.yRot = 0f
            model.leftArm.zRot = 0f
        }
    }
}
