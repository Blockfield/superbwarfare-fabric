package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import org.apache.logging.log4j.Logger
import java.util.function.Consumer

// 仅在客户端资源重载时记录一次的Logger
class ResourceOnceLogger {
    private val logged = HashSet<Any>()

    init {
        LOGGERS.add(this)
    }

    fun log(
        obj: Any,
        logger: Consumer<Logger>,
    ) {
        if (logged.contains(obj)) {
            return
        }
        logged.add(obj)
        logger.accept(Mod.LOGGER)
    }

    internal class ReloadListener : SimpleSynchronousResourceReloadListener {
        override fun getFabricId(): ResourceLocation = Mod.loc("resource_once_logger")

        override fun onResourceManagerReload(resourceManager: ResourceManager) {
            LOGGERS.forEach { it?.logged?.clear() }
        }
    }

    companion object {
        private val INSTANCE = ReloadListener()
        private val LOGGERS = ArrayList<ResourceOnceLogger?>()

        fun init() {
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE)
        }
    }
}
