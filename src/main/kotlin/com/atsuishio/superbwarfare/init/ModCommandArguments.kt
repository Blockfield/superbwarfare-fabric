package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.LowerCamelCaseEnumArgument
import com.atsuishio.superbwarfare.fabric.DeferredRegister
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation

object ModCommandArguments {
    /**
     * Пустой: ArgumentTypeRegistry сам кладёт инфо и в реестр, и в карту класс -> инфо
     * (аналог ArgumentTypeInfos.registerByClass из NeoForge). Поле оставлено точкой входа
     * из Mod.kt: обращение к нему инициализирует объект и запускает init ниже.
     */
    @JvmStatic
    val COMMAND_ARGUMENT_TYPES: DeferredRegister<ArgumentTypeInfo<*, *>> =
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Mod.MODID)

    init {
        ArgumentTypeRegistry.registerArgumentType(
            ResourceLocation.fromNamespaceAndPath(Mod.MODID, "lower_camel_case_enum"),
            LowerCamelCaseEnumArgument::class.java,
            LowerCamelCaseEnumArgument.Info(),
        )
    }
}
