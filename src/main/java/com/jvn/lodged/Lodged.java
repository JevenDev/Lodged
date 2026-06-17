package com.jvn.lodged;

import com.mojang.logging.LogUtils;
import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.effect.BleedingEvents;
import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.particle.LodgedParticles;
import com.jvn.lodged.world.LodgedArrowEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Lodged.MOD_ID)
public final class Lodged {
    public static final String MOD_ID = "lodged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Lodged(IEventBus modEventBus, ModContainer modContainer) {
        LodgedConfig.load();
        LodgedEffects.EFFECTS.register(modEventBus);
        LodgedParticles.PARTICLES.register(modEventBus);
        modEventBus.addListener(LodgedNetwork::registerPayloads);
        NeoForge.EVENT_BUS.register(BleedingEvents.class);
        NeoForge.EVENT_BUS.register(LodgedArrowEvents.class);
    }
}
