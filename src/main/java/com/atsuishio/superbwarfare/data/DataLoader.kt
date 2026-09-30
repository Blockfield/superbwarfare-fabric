package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.ModColor.ModColorAdapter
import com.atsuishio.superbwarfare.data.StringOrVec3.StringOrVec3Adapter
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel.LimitAdapter
import com.atsuishio.superbwarfare.network.message.receive.DataSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.github.fabricators_of_create.porting_lib.entity.events.OnDatapackSyncCallback
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.players.PlayerList
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Consumer

object DataLoader {
    @JvmField
    val GSON: Gson = createCommonBuilder().create()

    @OptIn(ExperimentalSerializationApi::class)
    val JSON =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            serializersModule = com.atsuishio.superbwarfare.serialization.serializersModule
            allowTrailingComma = true
            allowSpecialFloatingPointValues = true
        }

    @JvmField
    val JSON_OBJECT_CACHE: LoadingCache<Any, JsonObject> =
        CacheBuilder
            .newBuilder()
            .weakKeys()
            .build(
                object : CacheLoader<Any, JsonObject>() {
                    override fun load(obj: Any): JsonObject = GSON.toJsonTree(obj).getAsJsonObject()
                },
            )

    val LOADED_DATA = mutableMapOf<String, GeneralData<*>>()
    val LOADED_RESOURCE = mutableMapOf<String, GeneralData<*>>()

    val SERVER_LISTENER: ComplexJsonResourceReloadListener = ComplexJsonResourceReloadListener(LOADED_DATA)
    val CLIENT_LISTENER: ComplexJsonResourceReloadListener = ComplexJsonResourceReloadListener(LOADED_RESOURCE)

    fun init() {
        ResourceManagerHelper
            .get(PackType.SERVER_DATA)
            .registerReloadListener(Identified("data", SERVER_LISTENER))
        OnDatapackSyncCallback.EVENT.register { playerList, player -> onDataPackSync(playerList, player) }
    }

    @Suppress("unchecked_cast")
    @JvmOverloads
    fun <T> createData(
        directory: String,
        clazz: Class<T>,
        synced: Boolean = false,
        isKtData: Boolean = false,
        onReload: Consumer<Map<String, Any>>? = null,
    ): DataMap<T> {
        val data = LOADED_DATA[directory]

        if (data != null) {
            return data.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_DATA)
            LOADED_DATA[directory] = GeneralData(clazz, proxyMap, HashMap(), synced, isKtData, onReload)
            return proxyMap
        }
    }

    @Suppress("unchecked_cast")
    @JvmOverloads
    fun <T> createResource(
        directory: String,
        clazz: Class<T>,
        isKtData: Boolean = false,
        onReload: Consumer<Map<String, Any>>? = null,
    ): DataMap<T> {
        val resource = LOADED_RESOURCE[directory]

        if (resource != null) {
            return resource.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_RESOURCE)
            LOADED_RESOURCE[directory] = GeneralData(clazz, proxyMap, HashMap(), false, isKtData, onReload)
            return proxyMap
        }
    }

    // 务必在所有需要序列化GSON数据的地方调用，避免报错
    @JvmStatic
    fun createCommonBuilder(): GsonBuilder =
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .setLenient()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(Vec2::class.java, Vec2Adapter())
            .registerTypeAdapter(Vec3::class.java, Vec3Adapter())
            .registerTypeAdapter(ResourceLocation::class.java, ResourceLocationAdapter())
            .registerTypeAdapter(SoundEvent::class.java, SoundEventAdapter())
            .registerTypeAdapter(ModColor::class.java, ModColorAdapter())
            .registerTypeAdapter(StringOrVec3::class.java, StringOrVec3Adapter())
            .registerTypeAdapter(CollisionLevel.Limit::class.java, LimitAdapter())
            .registerTypeAdapterFactory(ObjectToList.AdapterFactory())
            .registerTypeAdapterFactory(StringToObject.AdapterFactory())

    /**
     * 将StringToObject和ObjectToList转换为原始值
     */
    @JvmStatic
    fun processValue(value: Any?): Any? =
        when (value) {
            is ObjectToList<*> -> value.list.map { value -> processValue(value) }
            is StringToObject<*> -> processValue(value.value)
            else -> value
        }

    data class GeneralData<T>(
        val type: Class<*>,
        val proxyMap: DataMap<T>,
        val dataMap: HashMap<String, Any>,
        val synced: Boolean,
        val isKtData: Boolean = false,
        val onReload: Consumer<Map<String, Any>>?,
    ) {
        val mapType by lazy {
            TypeToken.getParameterized(HashMap::class.java, String::class.java, type)!!
        }

        fun serializeToString(): String =
            if (isKtData) {
                JSON.encodeToString(serializer(mapType.type), dataMap)
            } else {
                GSON.toJson(dataMap)!!
            }
    }

    /** player == null означает перезагрузку датапаков: рассылаем всем, как делал NeoForge. */
    private fun onDataPackSync(
        playerList: PlayerList,
        player: ServerPlayer?,
    ) {
        val server = playerList.server
        val relevantPlayers: List<ServerPlayer> = player?.let { listOf(it) } ?: playerList.players

        LOADED_DATA.filter { it.value.synced }.forEach { (key, data) ->
            val packet = DataSyncMessage(key, data.serializeToString())

            for (p in relevantPlayers) {
                if (server.isSingleplayerOwner(p.gameProfile)) continue

                p.sendPacket(packet)
            }
        }
    }

    internal object ClientReloadListener {
        fun init() {
            ResourceManagerHelper
                .get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identified("resource", CLIENT_LISTENER))
        }
    }

    /**
     * ResourceManagerHelper принимает только IdentifiableResourceReloadListener, а
     * ComplexJsonResourceReloadListener наследуется от ванильного SimplePreparableReloadListener.
     * Обёртка добавляет недостающий id, не трогая сам слушатель.
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
