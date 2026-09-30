package com.atsuishio.superbwarfare.fabric

import com.atsuishio.superbwarfare.Mod
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.BlockModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.Material
import net.minecraft.client.resources.model.ModelBaker
import net.minecraft.client.resources.model.ModelState
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function

/**
 * Загрузчики моделей NeoForge, которых нет на Fabric. Разбираем те же JSON-файлы, что и апстрим,
 * поэтому ассеты править не нужно.
 *
 *  - `neoforge:separate_transforms` -- 2D-иконка в GUI, GeckoLib-модель в руке. Модель на
 *    перспективу подменяет [com.atsuishio.superbwarfare.mixins.ItemRendererMixin].
 *  - `neoforge:obj` (в том числе внутри `neoforge:composite`) -- геометрия из .obj, у Fabric
 *    OBJ-загрузчика нет вообще.
 *
 * ponytail: реализовано ровно то подмножество, которым пользуется мод.
 *  - `base`/`perspectives` понимаются только в форме `{"parent": "..."}` (так их и пишет
 *    датаген CustomSeparateModelBuilder); вложенная геометрия проигнорируется.
 *  - `composite` склеивает квады всех obj-детей и не умеет их скрывать по перспективе.
 *  - у OBJ читаются v/vt/vn/f/usemtl; .mtl не парсится -- текстуры берутся из `textures`
 *    самой модели, как в апстримовом JSON. `emissive_ambient`, `flip_v`, `automatic_culling`
 *    не поддержаны (мод их не задаёт).
 * Расширять -- только если в апстриме появится модель, которой этого мало.
 */
@Environment(EnvType.CLIENT)
object NeoForgeModelLoaders : PreparableModelLoadingPlugin<Map<ResourceLocation, UnbakedModel>> {
    /**
     * OBJ-конвенция V-координаты. Апстрим не задаёт `flip_v`, поэтому берём vt как есть.
     * Если текстура на dragon_teeth окажется отражённой по вертикали -- переключить здесь.
     */
    private const val FLIP_V = false

    fun init() {
        PreparableModelLoadingPlugin.register(Loader, this)
    }

    override fun onInitializeModelLoader(
        models: Map<ResourceLocation, UnbakedModel>,
        context: ModelLoadingPlugin.Context,
    ) {
        context.resolveModel().register(ModelResolver { ctx -> models[ctx.id()] })
    }

    private object Loader : PreparableModelLoadingPlugin.DataLoader<Map<ResourceLocation, UnbakedModel>> {
        override fun load(
            manager: ResourceManager,
            executor: Executor,
        ): CompletableFuture<Map<ResourceLocation, UnbakedModel>> = CompletableFuture.supplyAsync({ scan(manager) }, executor)
    }

    private fun scan(manager: ResourceManager): Map<ResourceLocation, UnbakedModel> {
        val result = HashMap<ResourceLocation, UnbakedModel>()
        val files = manager.listResources("models") { it.namespace == Mod.MODID && it.path.endsWith(".json") }

        for ((file, resource) in files) {
            val text =
                try {
                    resource.openAsReader().use { it.readText() }
                } catch (e: Exception) {
                    Mod.LOGGER.error("Не прочитана модель {}", file, e)
                    continue
                }
            if ("\"loader\"" !in text) continue

            val id =
                ResourceLocation.fromNamespaceAndPath(
                    file.namespace,
                    file.path.removePrefix("models/").removeSuffix(".json"),
                )
            try {
                val json = JsonParser.parseString(text).asJsonObject
                parse(json, text, manager)?.let { result[id] = it }
            } catch (e: Exception) {
                Mod.LOGGER.error("Не разобрана модель {} с кастомным загрузчиком", id, e)
            }
        }
        return result
    }

    private fun parse(
        json: JsonObject,
        text: String,
        manager: ResourceManager,
    ): UnbakedModel? =
        when (json.get("loader")?.asString) {
            "neoforge:separate_transforms" -> {
                parseSeparateTransforms(json)
            }

            "neoforge:obj" -> {
                parseObjModel(json, listOf(json), text, manager)
            }

            "neoforge:composite" -> {
                val children =
                    json
                        .getAsJsonObject("children")
                        ?.entrySet()
                        ?.mapNotNull { it.value as? JsonObject }
                        ?.filter { it.get("loader")?.asString == "neoforge:obj" }
                        .orEmpty()
                if (children.isEmpty()) null else parseObjModel(json, children, text, manager)
            }

            else -> {
                null
            }
        }

    // -------------------------------------------------------------- separate_transforms

    private fun parseSeparateTransforms(json: JsonObject): UnbakedModel? {
        val base =
            json
                .getAsJsonObject("base")
                ?.get("parent")
                ?.asString
                ?.let(ResourceLocation::parse)
                ?: return null

        val perspectives = LinkedHashMap<ItemDisplayContext, ResourceLocation>()
        json.getAsJsonObject("perspectives")?.entrySet()?.forEach { (name, value) ->
            val context = ItemDisplayContext.values().firstOrNull { it.serializedName == name }
            val parent = (value as? JsonObject)?.get("parent")?.asString
            if (context != null && parent != null) perspectives[context] = ResourceLocation.parse(parent)
        }

        return if (perspectives.isEmpty()) {
            DelegatingUnbakedModel(base)
        } else {
            SeparateTransformsUnbakedModel(base, perspectives)
        }
    }

    private class SeparateTransformsUnbakedModel(
        private val base: ResourceLocation,
        private val perspectives: Map<ItemDisplayContext, ResourceLocation>,
    ) : UnbakedModel {
        private val deps = (listOf(base) + perspectives.values).distinct()

        override fun getDependencies(): Collection<ResourceLocation> = deps

        override fun resolveParents(resolver: Function<ResourceLocation, UnbakedModel>) {
            deps.forEach { resolver.apply(it).resolveParents(resolver) }
        }

        override fun bake(
            baker: ModelBaker,
            sprites: Function<Material, TextureAtlasSprite>,
            state: ModelState,
        ): BakedModel? {
            val baseBaked = baker.bake(base, state) ?: return null
            val perspectiveBaked =
                perspectives
                    .mapNotNull { (context, model) -> baker.bake(model, state)?.let { context to it } }
                    .toMap()
            return SeparateTransformsBakedModel(baseBaked, perspectiveBaked)
        }
    }

    // ---------------------------------------------------------------------------- obj

    private fun parseObjModel(
        root: JsonObject,
        parts: List<JsonObject>,
        text: String,
        manager: ResourceManager,
    ): UnbakedModel? {
        val faces = ArrayList<ObjFace>()
        val textures = HashMap<String, ResourceLocation>()

        for (part in parts) {
            val objId = part.get("model")?.asString?.let(ResourceLocation::parse) ?: continue
            val objText =
                manager
                    .getResource(objId)
                    .orElse(null)
                    ?.let { res -> res.openAsReader().use { it.readText() } }
            if (objText == null) {
                Mod.LOGGER.error("Не найден OBJ {}", objId)
                continue
            }
            faces += parseObjGeometry(objText)
            part.getAsJsonObject("textures")?.entrySet()?.forEach { (name, value) ->
                textures[name.removePrefix("#")] = ResourceLocation.parse(value.asString)
            }
        }
        if (faces.isEmpty()) return null

        val outer = BlockModel.fromString(text)
        val particle =
            root
                .getAsJsonObject("textures")
                ?.get("particle")
                ?.asString
                ?.let(ResourceLocation::parse)
        return ObjUnbakedModel(faces, textures, particle, outer.transforms, outer.hasAmbientOcclusion())
    }

    /** Одна грань OBJ, уже приведённая к четырёхугольнику (треугольник дублирует последнюю вершину). */
    private class ObjFace(
        val material: String,
        val pos: FloatArray,
        val uv: FloatArray,
        val normal: FloatArray,
    )

    private val WHITESPACE = Regex("\\s+")

    private val MISSING_TEXTURE: ResourceLocation = ResourceLocation.withDefaultNamespace("missingno")

    private fun parseObjGeometry(text: String): List<ObjFace> {
        val positions = ArrayList<FloatArray>()
        val uvs = ArrayList<FloatArray>()
        val normals = ArrayList<FloatArray>()
        val faces = ArrayList<ObjFace>()
        var material = ""

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val t = line.split(WHITESPACE)
            when (t[0]) {
                "v" -> {
                    positions += floatArrayOf(t[1].toFloat(), t[2].toFloat(), t[3].toFloat())
                }

                "vt" -> {
                    uvs += floatArrayOf(t[1].toFloat(), t[2].toFloat())
                }

                "vn" -> {
                    normals += floatArrayOf(t[1].toFloat(), t[2].toFloat(), t[3].toFloat())
                }

                "usemtl" -> {
                    material = t.getOrElse(1) { "" }
                }

                "f" -> {
                    val refs = t.drop(1)
                    if (refs.size < 3) continue
                    val used = minOf(refs.size, 4)
                    val pos = FloatArray(12)
                    val uv = FloatArray(8)
                    val normal = FloatArray(3)

                    for (i in 0 until 4) {
                        val ref = refs[if (i < used) i else used - 1].split('/')
                        val v = positions[index(ref[0], positions.size)]
                        pos[i * 3] = v[0]
                        pos[i * 3 + 1] = v[1]
                        pos[i * 3 + 2] = v[2]

                        if (ref.size > 1 && ref[1].isNotEmpty()) {
                            val c = uvs[index(ref[1], uvs.size)]
                            uv[i * 2] = c[0]
                            uv[i * 2 + 1] = c[1]
                        }
                        if (ref.size > 2 && ref[2].isNotEmpty()) {
                            val n = normals[index(ref[2], normals.size)]
                            normal[0] += n[0]
                            normal[1] += n[1]
                            normal[2] += n[2]
                        }
                    }

                    if (normal[0] == 0f && normal[1] == 0f && normal[2] == 0f) crossNormal(pos, normal)
                    normalize(normal)
                    faces += ObjFace(material, pos, uv, normal)
                }
            }
        }
        return faces
    }

    /** OBJ-индексы с единицы, отрицательные -- от конца списка. */
    private fun index(
        token: String,
        size: Int,
    ): Int {
        val i = token.toInt()
        return if (i < 0) size + i else i - 1
    }

    private fun crossNormal(
        pos: FloatArray,
        out: FloatArray,
    ) {
        val ax = pos[3] - pos[0]
        val ay = pos[4] - pos[1]
        val az = pos[5] - pos[2]
        val bx = pos[6] - pos[0]
        val by = pos[7] - pos[1]
        val bz = pos[8] - pos[2]
        out[0] = ay * bz - az * by
        out[1] = az * bx - ax * bz
        out[2] = ax * by - ay * bx
    }

    private fun normalize(n: FloatArray) {
        val len = Math.sqrt((n[0] * n[0] + n[1] * n[1] + n[2] * n[2]).toDouble()).toFloat()
        if (len > 1.0e-5f) {
            n[0] /= len
            n[1] /= len
            n[2] /= len
        } else {
            n[1] = 1f
        }
    }

    private class ObjUnbakedModel(
        private val faces: List<ObjFace>,
        private val textures: Map<String, ResourceLocation>,
        private val particle: ResourceLocation?,
        private val transforms: ItemTransforms,
        private val ambientOcclusion: Boolean,
    ) : UnbakedModel {
        override fun getDependencies(): Collection<ResourceLocation> = emptyList()

        override fun resolveParents(resolver: Function<ResourceLocation, UnbakedModel>) {}

        override fun bake(
            baker: ModelBaker,
            sprites: Function<Material, TextureAtlasSprite>,
            state: ModelState,
        ): BakedModel {
            fun spriteOf(texture: ResourceLocation): TextureAtlasSprite = sprites.apply(Material(TextureAtlas.LOCATION_BLOCKS, texture))

            val fallback = particle ?: textures.values.firstOrNull()
            val cache = HashMap<String, TextureAtlasSprite>()
            val quads =
                faces.mapNotNull { face ->
                    val texture = textures[face.material] ?: fallback ?: return@mapNotNull null
                    bakeQuad(face, cache.getOrPut(face.material) { spriteOf(texture) })
                }
            val particleSprite = spriteOf(particle ?: fallback ?: MISSING_TEXTURE)
            return ObjBakedModel(quads, particleSprite, transforms, ambientOcclusion)
        }
    }

    private fun bakeQuad(
        face: ObjFace,
        sprite: TextureAtlasSprite,
    ): BakedQuad {
        val data = IntArray(32)
        for (i in 0 until 4) {
            val o = i * 8
            data[o] = java.lang.Float.floatToRawIntBits(face.pos[i * 3])
            data[o + 1] = java.lang.Float.floatToRawIntBits(face.pos[i * 3 + 1])
            data[o + 2] = java.lang.Float.floatToRawIntBits(face.pos[i * 3 + 2])
            data[o + 3] = -1
            val v = if (FLIP_V) 1f - face.uv[i * 2 + 1] else face.uv[i * 2 + 1]
            data[o + 4] = java.lang.Float.floatToRawIntBits(sprite.getU(face.uv[i * 2]))
            data[o + 5] = java.lang.Float.floatToRawIntBits(sprite.getV(v))
            data[o + 6] = 0
            data[o + 7] = packNormal(face.normal)
        }
        val direction = Direction.getNearest(face.normal[0], face.normal[1], face.normal[2])
        return BakedQuad(data, -1, direction, sprite, true)
    }

    private fun packNormal(n: FloatArray): Int {
        val x = (n[0] * 127f).toInt() and 0xFF
        val y = (n[1] * 127f).toInt() and 0xFF
        val z = (n[2] * 127f).toInt() and 0xFF
        return (z shl 16) or (y shl 8) or x
    }

    /** Все квады отдаём как неотсекаемые: OBJ выходит за пределы блока и своих граней не имеет. */
    private class ObjBakedModel(
        private val quads: List<BakedQuad>,
        private val particle: TextureAtlasSprite,
        private val transforms: ItemTransforms,
        private val ambientOcclusion: Boolean,
    ) : BakedModel {
        override fun getQuads(
            state: BlockState?,
            side: Direction?,
            random: RandomSource,
        ): List<BakedQuad> = if (side == null) quads else emptyList()

        override fun useAmbientOcclusion(): Boolean = ambientOcclusion

        override fun isGui3d(): Boolean = true

        override fun usesBlockLight(): Boolean = true

        override fun isCustomRenderer(): Boolean = false

        override fun getParticleIcon(): TextureAtlasSprite = particle

        override fun getTransforms(): ItemTransforms = transforms

        override fun getOverrides(): ItemOverrides = ItemOverrides.EMPTY
    }
}

/**
 * Обёртка `neoforge:separate_transforms`: наружу ведёт себя как базовая модель, а
 * [forPerspective] отдаёт модель для конкретной перспективы. `usesBlockLight` спрашиваем
 * у GUI-модели: это ровно то, чем в апстриме был `gui_light: front` наверху модели, и читает
 * его только отрисовка в интерфейсе (плоский свет вместо блочного).
 */
@Environment(EnvType.CLIENT)
class SeparateTransformsBakedModel(
    private val base: BakedModel,
    private val perspectives: Map<ItemDisplayContext, BakedModel>,
) : BakedModel by base {
    private val gui = perspectives[ItemDisplayContext.GUI] ?: base

    fun forPerspective(context: ItemDisplayContext): BakedModel = perspectives[context] ?: base

    override fun usesBlockLight(): Boolean = gui.usesBlockLight()
}
