package com.jvn.lodged;

import com.mojang.logging.LogUtils;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.item.LodgedItems;
import com.jvn.lodged.network.BandageUseHandler;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import com.jvn.lodged.particle.LodgedParticles;
import com.jvn.lodged.world.LodgedArrowEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Lodged.MOD_ID)
public final class Lodged {
    public static final String MOD_ID = "lodged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Lodged(IEventBus modEventBus) {
        LodgedConfig.load();
        LodgedEffects.EFFECTS.register(modEventBus);
        LodgedItems.ITEMS.register(modEventBus);
        LodgedParticles.PARTICLES.register(modEventBus);
        modEventBus.addListener(LodgedItems::addCreativeTabItems);
        modEventBus.addListener(LodgedNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(BloodColors::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(BandageUseHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BandageUseHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(BandageUseHandler::onPlayerStartTracking);
        NeoForge.EVENT_BUS.addListener(BandageUseHandler::onServerStopped);
        NeoForge.EVENT_BUS.addListener(PlayerArrowRemoval::onEntityTick);
        NeoForge.EVENT_BUS.addListener(PlayerArrowRemoval::onServerStopped);
        NeoForge.EVENT_BUS.register(BleedingEvents.class);
        NeoForge.EVENT_BUS.register(LodgedArrowEvents.class);
    }
}
