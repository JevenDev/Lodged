package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.jvn.toucanlib.client.render.ToucanPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class LodgedDizzinessPostProcessor {
    public static final LodgedDizzinessPostProcessor INSTANCE = new LodgedDizzinessPostProcessor();
    private static final ResourceLocation POST_CHAIN_LOCATION =
            Lodged.IDS.id("shaders/post/dizziness_tint.json");

    private final ToucanPostChain postChain = new ToucanPostChain(
            POST_CHAIN_LOCATION,
            exception -> Lodged.LOGGER.error("Failed to load Lodged dizziness post-processing shader", exception));
    private float blend;
    private float tintStrength;
    private float pulseStrength;
    private float time;
    private boolean active;

    private LodgedDizzinessPostProcessor() {
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener reloadListener = resourceManager -> INSTANCE.reload();
        event.registerReloadListener(reloadListener);
    }

    public void reload() {
        postChain.reload();
        time = 0.0F;
    }

    public void updateState(float blend) {
        this.blend = blend;
        this.tintStrength = blend * (0.24F + blend * 0.18F);
        this.pulseStrength = blend * 0.018F;
        this.active = blend > 0.02F;
        if (!this.active) {
            time = 0.0F;
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            INSTANCE.render(event);
        }
    }

    private void render(RenderLevelStageEvent event) {
        if (!active) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !postChain.ensureLoaded(minecraft)) {
            return;
        }

        postChain.resizeIfNeeded(minecraft);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        time += partialTick / 20.0F;

        postChain.setUniform("time", time);
        postChain.setUniform("Blend", blend);
        postChain.setUniform("TintStrength", tintStrength);
        postChain.setUniform("PulseStrength", pulseStrength);
        postChain.processAndRestore(minecraft, partialTick);
    }
}
