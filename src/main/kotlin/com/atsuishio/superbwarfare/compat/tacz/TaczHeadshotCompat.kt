package com.atsuishio.superbwarfare.compat.tacz

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import net.minecraft.world.damagesource.DamageSource

/** PRE вызывается перед hurt(), включая смертельное попадание. Только серверный результат:
 * в одиночной игре клиентские события не должны перезаписывать состояние серверного потока.
 * Класс грузится только под isModLoaded("tacz"). */
object TaczHeadshotCompat {
    private var headshotBulletId = -1

    fun init() {
        EntityHurtByGunEvent.PRE.register { event ->
            if (event.logicalSide.isServer) recordHit(event.bullet.id, event.isHeadShot)
        }
    }

    internal fun recordHit(
        bulletId: Int,
        headshot: Boolean,
    ) {
        // Одна пробивающая пуля может после головы попасть в тело другой цели.
        headshotBulletId = if (headshot) bulletId else -1
    }

    internal fun isHeadshot(bulletId: Int?) = headshotBulletId != -1 && bulletId == headshotBulletId

    fun isHeadshot(source: DamageSource) = isHeadshot(source.directEntity?.id)
}
