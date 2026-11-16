package com.atsuishio.superbwarfare.compat.tacz

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import net.minecraft.world.damagesource.DamageSource

/** PRE вызывается перед hurt(), включая смертельное попадание. Только серверный результат:
 * в одиночной игре клиентские события не должны перезаписывать состояние серверного потока.
 * Класс грузится только под isModLoaded("tacz"). */
object TaczHeadshotCompat {
    private const val MAX_TRACKED = 256
    private val headshotByBullet = LinkedHashMap<Int, Boolean>()

    fun init() {
        EntityHurtByGunEvent.PRE.register { event ->
            if (event.logicalSide.isServer) recordHit(event.bullet.id, event.isHeadShot)
        }
    }

    internal fun recordHit(
        bulletId: Int,
        headshot: Boolean,
    ) {
        // Одна запись на пулю: чужой огонь и второй хит той же пули по другой цели
        // больше не стирают чужой хедшот. Переполнение сбрасывает старейшие.
        headshotByBullet[bulletId] = headshot
        while (headshotByBullet.size > MAX_TRACKED) {
            headshotByBullet.remove(headshotByBullet.keys.first())
        }
    }

    internal fun isHeadshot(bulletId: Int?) = bulletId != null && headshotByBullet[bulletId] == true

    fun isHeadshot(source: DamageSource) = isHeadshot(source.directEntity?.id)
}
