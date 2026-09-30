package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.command.CommandRegister
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.config.CLIENT_CONFIG
import com.atsuishio.superbwarfare.config.COMMON_CONFIG
import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
import com.atsuishio.superbwarfare.data.loot.WreckageLootDataManager
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.projectile.FastProjectileManualTicker
import com.atsuishio.superbwarfare.event.CustomEventHandler
import com.atsuishio.superbwarfare.event.EntityUseGunEventHandler
import com.atsuishio.superbwarfare.event.HitboxHelperEventHandler
import com.atsuishio.superbwarfare.event.LivingEventHandler
import com.atsuishio.superbwarfare.event.PlayerEventHandler
import com.atsuishio.superbwarfare.fabric.EntityHooks
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.item.curio.IffItem
import com.atsuishio.superbwarfare.item.misc.TowBarItem
import com.atsuishio.superbwarfare.item.misc.TowlineItem
import com.atsuishio.superbwarfare.item.weapon.HammerItem
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect
import com.atsuishio.superbwarfare.mobeffect.PhosphorusFireMobEffect
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect
import com.atsuishio.superbwarfare.mobeffect.TraumaMobEffect
import com.atsuishio.superbwarfare.network.initializeNetwork
import com.atsuishio.superbwarfare.perk.damage.BattleOfWits
import com.atsuishio.superbwarfare.perk.functional.PowerfulAttraction
import com.atsuishio.superbwarfare.procedures.WelcomeProcedure
import com.atsuishio.superbwarfare.recipe.ModPotionRecipes
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler
import com.atsuishio.superbwarfare.tools.postEvent
import com.atsuishio.superbwarfare.world.saveddata.ChunkPosSavedData
import com.atsuishio.superbwarfare.world.saveddata.ProjectileChunkSavedData
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.config.ModConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private typealias Task = AbstractMap.SimpleEntry<Runnable, Int>

object Mod : ModInitializer {
    const val MODID: String = "superbwarfare"

    @JvmField
    val ATTRIBUTE_MODIFIER: ResourceLocation = loc("attribute_modifier")

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Mod::class.java)

    @JvmStatic
    fun loc(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MODID, path)

    private val SERVER_QUEUE: MutableCollection<Task> = ConcurrentLinkedQueue()
    private val CLIENT_QUEUE: MutableCollection<Task> = ConcurrentLinkedQueue()

    @JvmStatic
    fun queueServerWork(
        tick: Int,
        action: Runnable,
    ) = SERVER_QUEUE.add(AbstractMap.SimpleEntry(action, tick))

    @JvmStatic
    fun queueClientWork(
        tick: Int,
        action: Runnable,
    ) = CLIENT_QUEUE.add(AbstractMap.SimpleEntry(action, tick))

    @JvmStatic
    fun executeWork(queue: MutableCollection<Task>) {
        queue.removeAll(
            queue
                .onEach { it.setValue(it.value - 1) }
                .filter { it.value <= 0 }
                .onEach { it.key.run() }
                .toSet(),
        )
    }

    @JvmStatic
    fun executeClientWork() = executeWork(CLIENT_QUEUE)

    override fun onInitialize() {
        NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, CLIENT_CONFIG)
        NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, COMMON_CONFIG)
        NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, SERVER_CONFIG)

        // На Fabric реестры открыты прямо здесь: обращение к объекту инициализирует его поля,
        // и регистрация происходит в этот момент. Порядок важен — предметы ссылаются на блоки.
        ModPerks.init()
        ModSerializers.REGISTRY.register(null)
        ModSounds.REGISTRY.register(null)
        ModBlocks.REGISTRY.register(null)
        ModBlockEntities.REGISTRY.register(null)
        ModItems.register(null)
        ModDataComponents.register(null)
        ModTabs.init()
        ModEntities.init()
        ModBiomeModifications.init()
        ModMobEffects.REGISTRY.register(null)
        ModParticleTypes.REGISTRY.register(null)
        ModPotions.register(null)
        ModMenuTypes.REGISTRY.register(null)
        ModVillagers.init()
        ModRecipes.register(null)
        ModArmorMaterial.MATERIALS.register(null)
        ModAttributes.init()
        ModCriteriaTriggers.REGISTRY.register(null)
        ModAttachments.ATTACHMENT_TYPES.register(null)
        ModCommandArguments.COMMAND_ARGUMENT_TYPES.register(null)

        initializeNetwork()
        // Замена onAddedToLevel/onRemovedFromLevel и IEntityWithComplexSpawn из NeoForge.
        EntityHooks.init()
        // Подписчики уже зарегистрированы выше; рассылаем до сборки вкладок, иначе список пуст.
        postEvent(RegisterContainersEvent())
        ModDatapackRegistries.register()
        ModLootModifier.init()
        ModCapabilities.init()

        // Обработчики: у NeoForge их находила шина по аннотациям, на Fabric регистрируем явно.
        CustomEventHandler.init()
        EntityUseGunEventHandler.init()
        HitboxHelperEventHandler.init()
        PlayerEventHandler.init()
        LivingEventHandler.init()

        ContainerBlockItem.init()
        IffItem.init()
        TowlineItem.init()
        TowBarItem.init()
        HammerItem.init()

        DataLoader.init()
        ContainerDataManager.init()
        WreckageLootDataManager.init()
        PlayerVariable.init()
        VehicleAssemblingTableBlock.init()
        CommandRegister.init()
        CompatHolder.init()
        TargetEntity.init()
        DPSGeneratorEntity.init()
        FastProjectileManualTicker.init()
        ShockMobEffect.init()
        BurnMobEffect.init()
        PhosphorusFireMobEffect.init()
        TraumaMobEffect.init()
        PowerfulAttraction.init()
        BattleOfWits.init()
        ModPotionRecipes.init()
        WelcomeProcedure.init()
        ServerSyncedEntityHandler.init()
        ChunkPosSavedData.init()
        TDMSavedData.init()
        ProjectileChunkSavedData.init()
        ModItems.registerDispenserBehavior()
        ModGameRules.bootstrap()

        ServerTickEvents.END_SERVER_TICK.register { executeWork(SERVER_QUEUE) }

        registerBuiltInResourcePack()
        CustomData.load()
    }

    private fun registerBuiltInResourcePack() {
        val container = FabricLoader.getInstance().getModContainer(MODID).orElse(null) ?: return
        ResourceManagerHelper.registerBuiltinResourcePack(
            loc("sbw_legacy"),
            container,
            Component.translatable("pack.superbwarfare.sbw_legacy"),
            ResourcePackActivationType.NORMAL,
        )
    }
}
