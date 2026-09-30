package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget
import net.fabricmc.fabric.api.attachment.v1.AttachmentType

/**
 * NeoForge-овские data attachments -> fabric-data-attachment-api-v1.
 *
 * Форма вызовов сохранена: `entity.getData(ModAttachments.PLAYER_VARIABLE)` работает как раньше,
 * только теперь это extension-функции ниже, а не патч в Entity. Kotlin-файлам нужен их импорт,
 * Java зовёт `ModAttachmentsKt.getData(entity, ...)`.
 *
 * Персистентность на Fabric описывается Codec'ом, а не INBTSerializable, поэтому оба класса
 * отдают/принимают CompoundTag через xmap — NBT на диске остаётся тем же, что у апстрима.
 */
object ModAttachments {
    @JvmField
    val PLAYER_VARIABLE: AttachmentType<PlayerVariable> =
        AttachmentRegistry
            .builder<PlayerVariable>()
            .initializer(::PlayerVariable)
            .persistent(PlayerVariable.CODEC)
            .buildAndRegister(loc("player_variable"))

    @JvmField
    val PHOSPHORUS_FIRE: AttachmentType<PhosphorusFireCapability> =
        AttachmentRegistry
            .builder<PhosphorusFireCapability>()
            .initializer(::PhosphorusFireCapability)
            .persistent(PhosphorusFireCapability.CODEC)
            .buildAndRegister(loc("phosphorus_fire"))

    /**
     * Апстримовый Mod.kt зовёт `ModAttachments.ATTACHMENT_TYPES.register(bus)`. На Fabric типы
     * регистрируются при инициализации объекта, но обращение к полю её и запускает — поэтому
     * вызов оставлен как есть, а не выпилен из Mod.kt.
     */
    @JvmField
    val ATTACHMENT_TYPES: Registrar = Registrar

    object Registrar {
        @JvmStatic
        fun register(bus: Any?) = Unit
    }
}

/** Аналог NeoForge `IAttachmentHolder.getData`: создаёт значение по умолчанию и запоминает его. */
fun <T : Any> AttachmentTarget.getData(type: AttachmentType<T>): T = getAttachedOrCreate(type)

/** Аналог NeoForge `IAttachmentHolder.setData`. */
fun <T : Any> AttachmentTarget.setData(
    type: AttachmentType<T>,
    value: T,
): T? = setAttached(type, value)

/** Аналог NeoForge `IAttachmentHolder.hasData`. */
fun AttachmentTarget.hasData(type: AttachmentType<*>): Boolean = hasAttached(type)
