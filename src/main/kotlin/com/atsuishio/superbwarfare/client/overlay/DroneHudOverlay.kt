package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.IFFOverlay.FRIENDLY_ARTILLERY
import com.atsuishio.superbwarfare.client.overlay.IFFOverlay.FRIENDLY_INDICATOR
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3

@Environment(EnvType.CLIENT)
object DroneHudOverlay : CommonOverlay("drone_hud") {
    private val FRAME = loc("textures/overlay/frame/frame.png")
    private val TV_FRAME = loc("textures/overlay/vehicle/land/tv_frame.png")
    private val CROSSHAIR = loc("textures/overlay/vehicle/crosshair/third_camera.png")
    private val DRONE_FOV = loc("textures/overlay/drone/drone_fov.png")
    private val DRONE_FOV_MOVE = loc("textures/overlay/drone/drone_fov_move.png")

    private val INDICATOR = loc("textures/overlay/spyglass/indicator.png")

    /**
     * Доля помех 0..1: последняя пятая часть дальности связи. 0 на 80% дистанции, 1 на пределе.
     */
    private fun signalNoise(
        distance: Double,
        drone: DroneEntity,
    ): Float {
        val max = drone.maxControlDistance
        val start = 0.8 * max
        if (max <= start || distance <= start) return 0f
        return Mth.clamp(((distance - start) / (max - start)).toFloat(), 0f, 1f)
    }

    /** Шумовая текстура 64x64 генерируется один раз в рантайме: ассет в пак тащить не за чем. */
    private fun noiseTexture(): ResourceLocation {
        if (noiseTextureReady) return NOISE
        noiseTextureReady = true
        val image = NativeImage(NOISE_SIZE, NOISE_SIZE, false)
        val random = RandomSource.create(0x5EED)
        for (y in 0 until NOISE_SIZE) {
            for (x in 0 until NOISE_SIZE) {
                val v = 30 + random.nextInt(226)
                // NativeImage хранит ABGR; шум серый, так что порядок каналов роли не играет.
                image.setPixelRGBA(x, y, (0xFF shl 24) or (v shl 16) or (v shl 8) or v)
            }
        }
        mc.textureManager.register(NOISE, DynamicTexture(image))
        return NOISE
    }

    /**
     * Телевизионные помехи: шумовая текстура тайлами со случайным сдвигом каждый кадр, поверх --
     * разрывы строк, а выше 90% ещё и вспышки сплошного шума. Треск идёт UI-звуком: Player.playSound
     * уходит в level.playSound(this, ...), а клиент глушит звук, источник которого -- он сам.
     */
    private fun RenderContext.renderSignalNoise(strength: Float) {
        if (strength <= 0f) return
        val random = RandomSource.create()
        val texture = noiseTexture()
        val flicker = strength > 0.9f && random.nextFloat() < 0.15f
        val alpha = if (flicker) 1f else Mth.clamp(0.9f * strength, 0f, 0.9f)

        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
        val tile = NOISE_SIZE * NOISE_SCALE
        val offsetX = random.nextInt(tile)
        val offsetY = random.nextInt(tile)
        var x = -offsetX
        while (x < screenWidth) {
            var y = -offsetY
            while (y < screenHeight) {
                RenderHelper.preciseBlit(
                    guiGraphics,
                    texture,
                    x.toFloat(),
                    y.toFloat(),
                    0f,
                    0f,
                    tile.toFloat(),
                    tile.toFloat(),
                    NOISE_SIZE.toFloat(),
                    NOISE_SIZE.toFloat(),
                )
                y += tile
            }
            x += tile
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        // Разрыв строк: срезы кадра, сдвинутые вбок, как у сорванной развёртки.
        repeat(2 + (2 * strength).toInt()) {
            val bandY = random.nextInt(screenHeight)
            val bandH = 1 + random.nextInt(3)
            val shift = random.nextInt(40) - 20
            val bandAlpha = (120 + 120 * strength).toInt().coerceIn(0, 255)
            guiGraphics.fill(shift, bandY, screenWidth + shift, bandY + bandH, (bandAlpha shl 24) or 0xDCDCDC)
        }

        val level = mc.level ?: return
        val now = level.gameTime
        val interval = (8 - 6 * strength).toLong().coerceAtLeast(2L)
        if (now - lastNoiseSound < interval) return
        lastNoiseSound = now
        mc.soundManager.play(
            SimpleSoundInstance.forUI(
                SoundEvents.FIRE_EXTINGUISH,
                1.3f + 0.7f * random.nextFloat(),
                0.6f + 0.4f * strength,
            ),
        )
    }

    private const val LOSS_STATIC_TICKS = 10L
    private const val NOISE_SIZE = 64
    private const val NOISE_SCALE = 3
    private val NOISE = loc("dynamic/drone_signal_static")
    private var noiseTextureReady = false
    private var lastNoiseSound = 0L
    private var lastStrength = 0f
    private var lastDroneTick = 0L

    val maxDistance: Int
        get() {
            return (mc.level?.serverSimulationDistance ?: 16) * 16
        }

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        val stack = player.mainHandItem

        poseStack.pushPose()

        // Обрыв связи на грани: дрон уже исчез (подрыв, отвязка монитора), но полсекунды экран
        // держит сплошной шум -- иначе картинка просто мгновенно возвращается к рукам оператора.
        val gameTime = mc.level?.gameTime ?: 0L
        if (lastStrength >= 0.5f) {
            val sinceDrone = gameTime - lastDroneTick
            if (sinceDrone in 1..LOSS_STATIC_TICKS) {
                renderSignalNoise(1f)
                poseStack.popPose()
                return
            }
            if (sinceDrone > LOSS_STATIC_TICKS) lastStrength = 0f
        }

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        val tag = NBTTool.getTag(stack)
        val firstPerson =
            Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON ||
                Minecraft.getInstance().options.cameraType == CameraType.THIRD_PERSON_BACK

        if (stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            if (firstPerson) {
                guiGraphics.blit(CROSSHAIR, screenWidth / 2 - 16, screenHeight / 2 - 16, 0f, 0f, 32, 32, 32, 32)
                guiGraphics.blit(DRONE_FOV, screenWidth / 2 + 100, screenHeight / 2 - 64, 0f, 0f, 64, 129, 64, 129)
                val addW = (screenWidth / screenHeight) * 48
                val addH = (screenWidth / screenHeight) * 27
                RenderHelper.preciseBlit(
                    guiGraphics,
                    TV_FRAME,
                    -addW.toFloat() / 2,
                    -addH.toFloat() / 2,
                    10f,
                    0f,
                    0f,
                    (screenWidth + addW).toFloat(),
                    (screenHeight + addH).toFloat(),
                    (screenWidth + addW).toFloat(),
                    (screenHeight + addH).toFloat(),
                )

                RenderHelper.preciseBlit(
                    guiGraphics,
                    DRONE_FOV_MOVE,
                    screenWidth.toFloat() / 2 + 100,
                    (screenHeight / 2f - 64 - ((ClientEventHandler.droneFovLerp - 1) * 23.8)).toFloat(),
                    0f,
                    0f,
                    64f,
                    129f,
                    64f,
                    129f,
                )
                guiGraphics.drawString(
                    mc.font,
                    Component.literal(format1D(ClientEventHandler.droneFovLerp, "x")),
                    screenWidth / 2 + 144,
                    screenHeight / 2 + 56 - ((ClientEventHandler.droneFovLerp - 1) * 23.8).toInt(),
                    -1,
                    false,
                )

                val entity = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"))

                if (entity != null) {
                    var lookAtEntity = false
                    val distance = player.position().subtract(entity.position()).horizontalDistance()

                    val result =
                        entity.level().clip(
                            ClipContext(
                                cameraPos,
                                cameraPos.add(entity.getViewVector(1f).scale(512.0)),
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                entity,
                            ),
                        )
                    val hitPos = result.getLocation()

                    val blockRange = cameraPos.distanceTo(hitPos)

                    var entityRange = 0.0

                    val lookingEntity = TraceTool.droneFindLookingEntity(entity, cameraPos, 512.0, partialTick)
                    if (lookingEntity != null) {
                        lookAtEntity = true
                        entityRange = entity.distanceTo(lookingEntity).toDouble()
                    }

                    var color = -1

                    // Помехи связи: шум и разрывы строк тем плотнее, чем ближе граница связи.
                    lastStrength = signalNoise(distance, entity)
                    lastDroneTick = mc.level?.gameTime ?: 0L
                    renderSignalNoise(lastStrength)

                    // 超出距离警告
                    if (distance > maxDistance - 48) {
                        guiGraphics.drawString(
                            mc.font,
                            Component.translatable("tips.superbwarfare.drone.warning"),
                            screenWidth / 2 - 18,
                            screenHeight / 2 - 47,
                            -65536,
                            false,
                        )
                        color = -65536
                    }

                    // 距离
                    guiGraphics.drawString(
                        mc.font,
                        Component
                            .translatable("tips.superbwarfare.drone.distance")
                            .append(Component.literal(format1D(distance, "m"))),
                        screenWidth / 2 + 10,
                        screenHeight / 2 + 33,
                        color,
                        false,
                    )

                    // 血量
                    guiGraphics.drawString(
                        mc.font,
                        Component
                            .translatable("tips.superbwarfare.drone.health")
                            .append(
                                Component.literal(
                                    format1D(entity.health.toDouble()) + " / " +
                                        format1D(
                                            entity.getMaxHealth().toDouble(),
                                        ),
                                ),
                            ),
                        screenWidth / 2 - 77,
                        screenHeight / 2 + 33,
                        -1,
                        false,
                    )
                    if (!entity.getEntityData().get(DroneEntity.IS_KAMIKAZE)) {
                        // 弹药
                        guiGraphics.drawString(
                            mc.font,
                            Component
                                .translatable("tips.superbwarfare.drone.ammo")
                                .append(
                                    Component.literal(
                                        entity.getAmmo().toString() + " / " + entity.getEntityData().get(DroneEntity.MAX_AMMO),
                                    ),
                                ),
                            screenWidth / 2 + 12,
                            screenHeight / 2 - 37,
                            -1,
                            false,
                        )
                    } else {
                        // 神风
                        guiGraphics.drawString(
                            mc.font,
                            Component.translatable("tips.superbwarfare.drone.kamikaze"),
                            screenWidth / 2 + 12,
                            screenHeight / 2 - 37,
                            -65536,
                            false,
                        )
                    }

                    if (lookAtEntity) {
                        // 实体距离
                        guiGraphics.drawString(
                            mc.font,
                            Component
                                .translatable("tips.superbwarfare.drone.range")
                                .append(
                                    Component.literal(
                                        format1D(
                                            entityRange,
                                            "m ",
                                        ) + lookingEntity!!.displayName!!.string,
                                    ),
                                ),
                            screenWidth / 2 + 12,
                            screenHeight / 2 - 28,
                            color,
                            false,
                        )
                    } else {
                        // 方块距离
                        if (blockRange > 500) {
                            guiGraphics.drawString(
                                mc.font,
                                Component
                                    .translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal("---m")),
                                screenWidth / 2 + 12,
                                screenHeight / 2 - 28,
                                color,
                                false,
                            )
                        } else {
                            guiGraphics.drawString(
                                mc.font,
                                Component
                                    .translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(format1D(blockRange, "m"))),
                                screenWidth / 2 + 12,
                                screenHeight / 2 - 28,
                                color,
                                false,
                            )
                        }
                    }

                    val entities = SeekTool.seekLivingEntities(entity, 256.0, 30.0)
                    for (e in entities) {
                        val pos =
                            Vec3(
                                Mth.lerp(partialTick.toDouble(), e.xo, e.x),
                                Mth.lerp(partialTick.toDouble(), e.yo + e.eyeHeight, e.eyeY),
                                Mth.lerp(partialTick.toDouble(), e.zo, e.z),
                            )
                        val point = pos.worldToScreen()
                        poseStack.pushPose()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()

                        // Blockfield: own frame for self and allies; everyone else keeps the white one.
                        if (e == player || e.isAlliedTo(player)) {
                            RenderSystem.setShaderColor(0.2f, 1f, 0.2f, 1f)
                        } else {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        }
                        RenderHelper.preciseBlit(
                            guiGraphics,
                            FRAME,
                            x - 12,
                            y - 12,
                            24f,
                            24f,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                        )
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        poseStack.popPose()
                    }
                }

                // 射击诸元标记
                val offStack = player.offhandItem
                if (offStack.`is`(ModItems.FIRING_PARAMETERS, ModItems.ARTILLERY_INDICATOR)) {
                    val parameters = offStack.firingParameters
                    val blockPos = parameters.pos

                    val targetX = blockPos.x.toDouble()
                    val targetY = blockPos.y.toDouble()
                    val targetZ = blockPos.z.toDouble()

                    RenderSystem.disableDepthTest()
                    RenderSystem.depthMask(false)
                    RenderSystem.enableBlend()
                    RenderSystem.setShader { GameRenderer.getPositionTexShader() }
                    RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO,
                    )
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

                    // 标记位置
                    val pos = Vec3(targetX, targetY, targetZ)
                    if (pos.canBeSeen()) {
                        val point = pos.worldToScreen()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()
                        RenderHelper.preciseBlit(
                            guiGraphics,
                            INDICATOR,
                            Mth.clamp(x - 6, 0f, (screenWidth - 12).toFloat()),
                            Mth.clamp(y - 6, 0f, (screenHeight - 12).toFloat()),
                            0f,
                            0f,
                            12f,
                            12f,
                            12f,
                            12f,
                        )
                    }

                    // 火炮位置
                    if (offStack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                        val tags =
                            NBTTool
                                .getTag(offStack)
                                .getList(ArtilleryIndicatorItem.TAG_CANNON, Tag.TAG_COMPOUND.toInt())
                        for (m in tags.indices) {
                            val tag = tags.getCompound(m)
                            val e = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"))
                            if (e != null && e.position().canBeSeen()) {
                                val posF = lerpGetEntityBoundingBoxCenter(e, partialTick)
                                val pointF = posF.worldToScreen()
                                val xf = pointF.x.toFloat()
                                val yf = pointF.y.toFloat()

                                RenderHelper.preciseBlit(
                                    guiGraphics,
                                    FRIENDLY_ARTILLERY,
                                    Mth.clamp(xf - 6, 0f, (screenWidth - 12).toFloat()),
                                    Mth.clamp(yf - 6, 0f, (screenHeight - 12).toFloat()),
                                    0f,
                                    0f,
                                    12f,
                                    12f,
                                    12f,
                                    12f,
                                )
                            }
                        }
                    }
                }
            } else {
                if (player.position().canBeSeen()) {
                    var team: Entity? = player
                    if (player.vehicle != null) {
                        team = player.vehicle
                    }
                    val pos = lerpGetEntityBoundingBoxCenter(team!!, partialTick)
                    val point = pos.worldToScreen()
                    val xf = point.x.toFloat()
                    val yf = point.y.toFloat()

                    RenderHelper.preciseBlit(
                        guiGraphics,
                        FRIENDLY_INDICATOR,
                        Mth.clamp(xf - 6, 0f, (screenWidth - 12).toFloat()),
                        Mth.clamp(yf - 6, 0f, (screenHeight - 12).toFloat()),
                        0f,
                        0f,
                        12f,
                        12f,
                        12f,
                        12f,
                    )
                }
            }
        }

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        poseStack.popPose()
    }
}
