package com.atsuishio.superbwarfare.fabric

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.util.function.Supplier

/**
 * Замена DeferredRegister/DeferredHolder из NeoForge с той же формой вызовов, чтобы в файлах
 * пакета init менялся только импорт, а не логика регистрации.
 *
 * На Fabric реестры открыты во время инициализации мода, поэтому регистрация происходит сразу,
 * а register(bus) остаётся точкой, которая принудительно инициализирует объект-владелец:
 * поля Kotlin-объекта ленивы и без обращения к ним ничего бы не зарегистрировалось.
 *
 * ponytail: регистрация ранняя, а не отложенная. Если между объектами init появится цикл
 * (ModItems тянет ModBlocks и наоборот), разводить порядком вызовов register() в Mod.kt.
 */
class DeferredHolder<R, T : R>(
    private val reference: Holder.Reference<R>,
    private val value: T,
) : Holder<R> by reference,
    Supplier<T> {
    override fun get(): T = value

    // Свойствами, а не функциями: Java по-прежнему видит getKey()/getId(), а Kotlin
    // получает holder.id -- форму, которой пользуется апстрим.
    val key: ResourceKey<R>? get() = reference.key()

    val id: ResourceLocation get() = reference.key().location()

    override fun toString(): String = "DeferredHolder[$id]"
}

open class DeferredRegister<T> protected constructor(
    private val registry: Registry<T>,
    private val namespace: String,
) {
    private val registered = mutableListOf<DeferredHolder<T, out T>>()

    /** Апстрим ходит сюда и как .entries из Kotlin, и как getEntries() из Java. */
    val entries: Collection<DeferredHolder<T, out T>> get() = registered

    open fun <U : T> register(
        name: String,
        supplier: Supplier<U>,
    ): DeferredHolder<T, U> {
        val id = ResourceLocation.fromNamespaceAndPath(namespace, name)
        val value = supplier.get()
        val reference = Registry.registerForHolder(registry, id, value)
        val holder = DeferredHolder(reference, value)
        registered.add(holder)
        return holder
    }

    /** Только Supplier и (ResourceLocation) -> U; java.util.function.Function сюда не подходит. */
    fun <U : T> register(
        name: String,
        factory: (ResourceLocation) -> U,
    ): DeferredHolder<T, U> = register(name) { factory(ResourceLocation.fromNamespaceAndPath(namespace, name)) }

    /** Совместимость с апстримом: на Fabric регистрировать уже нечего, объект инициализирован. */
    fun register(bus: Any?) = Unit

    companion object {
        @JvmStatic
        fun <T> create(
            registry: Registry<T>,
            namespace: String,
        ): DeferredRegister<T> = DeferredRegister(registry, namespace)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> create(
            key: ResourceKey<out Registry<T>>,
            namespace: String,
        ): DeferredRegister<T> {
            val registry =
                BuiltInRegistries.REGISTRY.get(key.location()) as? Registry<T>
                    ?: error("Реестра ${key.location()} нет в BuiltInRegistries")
            return DeferredRegister(registry, namespace)
        }
    }
}
