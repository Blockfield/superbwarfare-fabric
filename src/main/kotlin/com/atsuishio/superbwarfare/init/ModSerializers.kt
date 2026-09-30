package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.data.gun.GunData
import io.netty.buffer.ByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.phys.Vec3
import java.util.function.Supplier

/**
 * На Fabric нет реестра EntityDataSerializer: SynchedEntityData шлёт по сети числовой id из
 * статического списка EntityDataSerializers, поэтому регистрация идёт туда же. Порядок полей
 * объекта задаёт эти id, и он одинаков на клиенте и сервере — менять порядок нельзя.
 */
object ModSerializers {
    /** Форма вызова из Mod.kt сохранена: обращение к REGISTRY инициализирует объект, а с ним
     *  и регистрацию сериализаторов; самому register() делать нечего. */
    val REGISTRY get() = this

    fun register(bus: Any?) = Unit

    @JvmField
    val INT_LIST_SERIALIZER: Supplier<EntityDataSerializer<List<Int>>> =
        add(EntityDataSerializer.forValueType(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())))

    @JvmField
    val FLOAT_LIST_SERIALIZER: Supplier<EntityDataSerializer<List<Float>>> =
        add(EntityDataSerializer.forValueType(ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list())))

    @JvmField
    val VEC3_SERIALIZER: Supplier<EntityDataSerializer<Vec3>> =
        add(
            EntityDataSerializer.forValueType(
                object : StreamCodec<ByteBuf, Vec3> {
                    override fun decode(buf: ByteBuf): Vec3 = Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())

                    override fun encode(
                        buf: ByteBuf,
                        vec: Vec3,
                    ) {
                        buf.writeDouble(vec.x)
                        buf.writeDouble(vec.y)
                        buf.writeDouble(vec.z)
                    }
                },
            ),
        )

    @JvmField
    val VEHICLE_GUN_DATA_MAP_SERIALIZER: Supplier<EntityDataSerializer<Map<String, GunData>>> =
        add(
            object : EntityDataSerializer<Map<String, GunData>> {
                override fun codec(): StreamCodec<in RegistryFriendlyByteBuf, Map<String, GunData>> =
                    ByteBufCodecs.map(
                        { HashMap(it) },
                        ByteBufCodecs.STRING_UTF8,
                        GunData.VEHICLE_GUN_STREAM_CODEC,
                    )

                override fun copy(map: Map<String, GunData>): Map<String, GunData> {
                    val newMap = HashMap<String, GunData>()
                    map.forEach { (key: String, value: GunData) -> newMap[key] = value.copy() }
                    return newMap
                }
            },
        )

    @JvmField
    val SHORT_LIST_LIST_SERIALIZER: Supplier<EntityDataSerializer<List<List<Short>>>> =
        add(
            EntityDataSerializer.forValueType(
                ByteBufCodecs.SHORT.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
            ),
        )

    private fun <T> add(serializer: EntityDataSerializer<T>): Supplier<EntityDataSerializer<T>> {
        EntityDataSerializers.registerSerializer(serializer)
        return Supplier { serializer }
    }
}
