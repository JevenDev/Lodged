package com.jvn.lodged;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.BloodColors;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.item.LodgedItems;
import com.jvn.lodged.network.BandageUseHandler;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.PlayerArrowRemoval;
import com.jvn.lodged.particle.LodgedParticles;
import com.jvn.lodged.world.LodgedArrowEvents;
import com.jvn.toucanlib.neoforge.event.ToucanEventBuses;
import com.jvn.toucanlib.util.ToucanIds;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Lodged.MOD_ID)
public final class Lodged {
    public static final String MOD_ID = "lodged";
    public static final ToucanIds IDS = ToucanIds.create(MOD_ID);
    public static final Logger LOGGER = LogUtils.getLogger();

    public Lodged(IEventBus modEventBus) {
        LodgedConfig.load();
        LodgedEffects.EFFECTS.register(modEventBus);
        LodgedItems.ITEMS.register(modEventBus);
        LodgedParticles.PARTICLES.register(modEventBus);
        ToucanEventBuses.on(modEventBus)
                .listener(LodgedItems::addCreativeTabItems)
                .listener(LodgedNetwork::registerPayloads);
        ToucanEventBuses.game()
                .listener(BloodColors::onAddReloadListeners)
                .listener(BandageUseHandler::onPlayerTick)
                .listener(BandageUseHandler::onPlayerLoggedOut)
                .listener(BandageUseHandler::onPlayerStartTracking)
                .listener(BandageUseHandler::onServerStopped)
                .listener(PlayerArrowRemoval::onEntityTick)
                .listener(PlayerArrowRemoval::onServerStopped)
                .register(BleedingEvents.class)
                .register(LodgedArrowEvents.class);
    }
}
