package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.resource.model.*
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object BedrockModelLoader {
    fun init() {
        val helper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
        helper.registerReloadListener(Identified("bedrock_model/vehicle", VehicleModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/vehicle_lod", VehicleLODModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/projectile", ProjectileModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/entity", EntityModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/armor", ArmorModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/block", BlockModelReloadListener))
        helper.registerReloadListener(Identified("bedrock_model/item", ItemModelReloadListener))
    }

    /**
     * ResourceManagerHelper принимает только IdentifiableResourceReloadListener, а слушатели
     * моделей наследуются от ванильного SimplePreparableReloadListener. Обёртка добавляет
     * недостающий id, ничего не меняя в самих слушателях.
     */
    private class Identified(
        name: String,
        private val delegate: PreparableReloadListener,
    ) : IdentifiableResourceReloadListener {
        private val id: ResourceLocation = Mod.loc(name)

        override fun getFabricId(): ResourceLocation = id

        override fun reload(
            barrier: PreparableReloadListener.PreparationBarrier,
            manager: ResourceManager,
            prepareProfiler: ProfilerFiller,
            applyProfiler: ProfilerFiller,
            prepareExecutor: Executor,
            applyExecutor: Executor,
        ): CompletableFuture<Void> = delegate.reload(barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor)
    }
}
