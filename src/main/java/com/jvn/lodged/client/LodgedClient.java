package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.particle.LodgedParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Lodged.MOD_ID, dist = Dist.CLIENT)
public final class LodgedClient {
    public LodgedClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(LodgedClient::registerParticleProviders);
        modEventBus.addListener(LodgedDizzinessPostProcessor::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onRenderGui);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessClientEffects::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(LodgedDizzinessPostProcessor::onRenderLevelStage);
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent));
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                LodgedParticles.BLEEDING_DROP.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        BleedingDropParticle.create(sprites, type, level, x, y, z, xSpeed, ySpeed, zSpeed));
    }
}
