package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.MouseMovementHandler
import com.atsuishio.superbwarfare.client.NvgHandler
import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter
import com.atsuishio.superbwarfare.client.map.TacticalMapChunkListener
import com.atsuishio.superbwarfare.client.overlay.GPWSOverlay
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.overlay.weapon.AircraftHud
import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud
import com.atsuishio.superbwarfare.client.overlay.weapon.OldAircraftHud
import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import com.atsuishio.superbwarfare.client.renderer.SyncedEntityWorldRenderer
import com.atsuishio.superbwarfare.client.renderer.curio.ParachuteRenderer
import com.atsuishio.superbwarfare.client.renderer.molang.MolangVariable
import com.atsuishio.superbwarfare.client.renderer.special.ContainerBlockPreview
import com.atsuishio.superbwarfare.client.renderer.special.TowingChainRenderer
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper
import com.atsuishio.superbwarfare.client.screens.SnapshotWarningScreen
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.event.ClickEventHandler
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.event.KillMessageHandler
import com.atsuishio.superbwarfare.fabric.EntityHooks
import com.atsuishio.superbwarfare.fabric.NeoForgeModelLoaders
import com.atsuishio.superbwarfare.init.ModEntityRenderers
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModParticles
import com.atsuishio.superbwarfare.init.ModProperties
import com.atsuishio.superbwarfare.init.ModScreens
import com.atsuishio.superbwarfare.init.ModSoundInstances
import com.atsuishio.superbwarfare.item.HandGrenade
import com.atsuishio.superbwarfare.item.LungeMine
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35Item
import com.atsuishio.superbwarfare.item.armor.HandsomeGogglesItem
import com.atsuishio.superbwarfare.item.armor.RuChest6b43Item
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47Item
import com.atsuishio.superbwarfare.item.armor.RuLeggingsItem
import com.atsuishio.superbwarfare.item.armor.UsChestIotvItem
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgtItem
import com.atsuishio.superbwarfare.item.armor.UsLeggingsItem
import com.atsuishio.superbwarfare.item.blockitem.BlueprintResearchTableBlockItem
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import com.atsuishio.superbwarfare.item.gun.GunGeoItem
import com.atsuishio.superbwarfare.item.misc.SkinSprayItem
import com.atsuishio.superbwarfare.item.projectile.PotionMortarShellItem
import com.atsuishio.superbwarfare.item.projectile.Ptkm1rItem
import com.atsuishio.superbwarfare.item.projectile.Tm62Item
import com.atsuishio.superbwarfare.item.weapon.MilitaryShovelItem
import com.atsuishio.superbwarfare.network.initializeClientNetwork
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.sound.SoundLimit
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object ModClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Не здесь: клиентский entrypoint зовётся из Minecraft.<init>, где mouseHandler ещё null,
        // и lateinit тихо получает null вместо значения. У NeoForge это делал FMLClientSetupEvent.
        ClientLifecycleEvents.CLIENT_STARTED.register { MouseMovementHandler.init() }
        MolangVariable.register()
        ModSoundInstances.init()
        SoundLimit.init()
        ModParticleRenderTypes.registerShaders()
        NeoForgeModelLoaders.init()
        initializeClientNetwork()
        EntityHooks.initClient()

        // Клиентские обработчики и рендер: у NeoForge их поднимала шина по аннотациям.
        ThermalShaderHandler.init()
        ClientRenderHandler.init()
        SyncedEntityWorldRenderer.init()
        ParachuteRenderer.init()
        TowingChainRenderer.init()
        ContainerBlockPreview.init()
        SnapshotWarningScreen.init()
        GPWSOverlay.init()
        OverlayTraceHandler.init()
        VehicleMainWeaponHudOverlay.init()
        AircraftHud.init()
        HelicopterHud.init()
        OldAircraftHud.init()
        FuMO25ScreenHelper.init()
        TacticalMapChunkListener.init()
        ClientLanguageGetter.init()
        KillMessageHandler.init()
        ClickEventHandler.init()
        ClientMouseHandler.init()
        ClientEventHandler.init()
        com.atsuishio.superbwarfare.client.util.PendingPlayerVariables
            .init()
        ModScreens.init()
        ModProperties.init()
        ModParticles.init()
        ModEntityRenderers.init()
        ModKeyMappings.init()
        GunGeoItem.init()
        RuChest6b43Item.init()
        GeHelmetM35Item.init()
        UsChestIotvItem.init()
        RuHelmet6b47Item.init()
        UsHelmetPasgtItem.init()
        HandsomeGogglesItem.init()
        RuLeggingsItem.init()
        UsLeggingsItem.init()
        NvgHandler.init()
        BlueprintResearchTableBlockItem.init()
        VehicleAssemblingTableBlockItem.init()
        SmallContainerBlockItem.init()
        LuckyContainerBlockItem.init()
        HandGrenade.init()
        LungeMine.init()
        SkinSprayItem.init()
        Ptkm1rItem.init()
        PotionMortarShellItem.init()
        Tm62Item.init()
        MilitaryShovelItem.init()
        BedrockModelLoader.init()
        ResourceOnceLogger.init()
        ContainerBlockItem.initClient()
        DataLoader.ClientReloadListener.init()

        ClientTickEvents.END_CLIENT_TICK.register { Mod.executeClientWork() }
    }
}
