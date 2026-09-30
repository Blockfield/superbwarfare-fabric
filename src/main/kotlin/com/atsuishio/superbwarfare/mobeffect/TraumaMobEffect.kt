package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.init.ModMobEffects
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingHurtEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity

object TraumaMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xF4ADB4) {
    fun init() {
        LivingHurtEvent.EVENT.register { onLivingHurt(it) }
    }

    /**
     * Аналога LivingHealEvent нет ни в Fabric API, ни в Porting Lib: лечение нигде не публикуется
     * как событие. Логика сохранена как чистая функция — она возвращает итоговое лечение
     * (0 = полностью подавить), остаётся вызвать её из миксина на LivingEntity#heal.
     */
    private fun modifyHealAmount(
        entity: LivingEntity,
        amount: Float,
    ): Float {
        val effect = entity.getEffect(ModMobEffects.TRAUMA) ?: return amount

        val amp = effect.amplifier + 1
        if (amp >= 10) {
            return 0f
        }

        return amount * (1 - amp * 0.1f)
    }

    private fun onLivingHurt(event: LivingHurtEvent) {
        val entity = event.entity
        val effect = entity.getEffect(ModMobEffects.TRAUMA) ?: return

        val amp = effect.amplifier + 1
        val amount = event.amount
        event.amount = amount * (1 + amp * 0.15f)
    }
}
