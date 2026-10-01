package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.fabric.DeferredRegister
import com.mojang.serialization.Lifecycle
import net.minecraft.core.MappedRegistry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.util.function.Supplier

fun deferredRegisterChecks() {
    val namespace = "superbwarfare_checks"
    val key = ResourceKey.createRegistryKey<String>(ResourceLocation.fromNamespaceAndPath(namespace, "registry"))
    val registry = MappedRegistry(key, Lifecycle.stable())
    val deferred = DeferredRegister.create(registry, namespace)
    var factoryCalls = 0
    val factoryId = ResourceLocation.fromNamespaceAndPath(namespace, "factory")
    val fromFactory =
        deferred.register("factory") { id ->
            factoryCalls++
            check(id == factoryId)
            "factory value"
        }
    check(factoryCalls == 1)
    check(fromFactory.id == factoryId && fromFactory.get() == "factory value")

    var supplierCalls = 0
    val fromSupplier =
        deferred.register(
            "supplier",
            Supplier {
                supplierCalls++
                "supplier value"
            },
        )
    check(supplierCalls == 1)
    registry.freeze()
    check(registry.get(factoryId) === fromFactory.get())
    check(registry.get(fromSupplier.id) === fromSupplier.get())
    check(deferred.entries.toList() == listOf(fromFactory, fromSupplier))
}
