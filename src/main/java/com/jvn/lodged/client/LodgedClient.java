package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Lodged.MOD_ID, dist = Dist.CLIENT)
public final class LodgedClient {
    public LodgedClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent));
    }
}
