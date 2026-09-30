package com.atsuishio.superbwarfare.client.language

import com.atsuishio.superbwarfare.Mod
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

@Environment(EnvType.CLIENT)
object ClientLanguageGetter {
    @JvmStatic
    lateinit var EN_US: ClientLanguage

    fun init() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            object :
                SimplePreparableReloadListener<ClientLanguage>(), IdentifiableResourceReloadListener {
                override fun getFabricId(): ResourceLocation = Mod.loc("client_language_getter")

                override fun prepare(
                    pResourceManager: ResourceManager,
                    pProfiler: ProfilerFiller,
                ): ClientLanguage = ClientLanguage.loadFrom(pResourceManager, listOf("en_us"), false)

                override fun apply(
                    pObject: ClientLanguage,
                    pResourceManager: ResourceManager,
                    pProfiler: ProfilerFiller,
                ) {
                    EN_US = pObject
                }
            },
        )
    }
}
