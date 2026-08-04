package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.particle.LodgedParticles;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Lodged.MOD_ID, dist = Dist.CLIENT)
public final class LodgedClient {
    public LodgedClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(LodgedClient::registerParticleProviders);
        modEventBus.addListener(BandageOverlayRenderer::registerLayerDefinitions);
        modEventBus.addListener(BandageOverlayRenderer::addPlayerLayers);
        modEventBus.addListener(LodgedBandageClient::registerKeyMappings);
        modEventBus.addListener(LodgedShieldArrowRemovalClient::registerKeyMappings);
        modEventBus.addListener(LodgedDizzinessPostProcessor::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(BandageOverlayRenderer::onRenderArm);
        NeoForge.EVENT_BUS.addListener(LodgedBandageClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LodgedShieldArrowRemovalClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LodgedArmorArrowTooltip::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onRenderGui);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessPostProcessor::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(LodgedClientStateEvents::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(LodgedClientStateEvents::onEntityLeaveLevel);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) LodgedClient::createConfigScreen);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                LodgedParticles.BLEEDING_DROP.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        BleedingDropParticle.create(sprites, type, level, x, y, z, xSpeed, ySpeed, zSpeed));
    }

    private static Screen createConfigScreen(ModContainer container, Screen parent) {
        var provider = ConfigScreenProviders.get(Lodged.MOD_ID);
        if (provider == null) {
            throw new IllegalStateException("Missing owo config screen provider for " + Lodged.MOD_ID);
        }
        return provider.apply(parent);
    }
}
