package com.jvn.lodged.client;

import com.google.gson.JsonParseException;
import com.jvn.lodged.Lodged;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class LodgedDizzinessPostProcessor {
    public static final LodgedDizzinessPostProcessor INSTANCE = new LodgedDizzinessPostProcessor();
    private static final ResourceLocation POST_CHAIN_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "shaders/post/dizziness_tint.json");

    private float blend;
    private float tintStrength;
    private float desaturationStrength;
    private float pulseStrength;
    private float time;
    private PostChain postChain;
    private boolean active;
    private boolean loadFailed;
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    private LodgedDizzinessPostProcessor() {
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener reloadListener = resourceManager -> INSTANCE.reload();
        event.registerReloadListener(reloadListener);
    }

    public void reload() {
        close();
        time = 0.0F;
        loadFailed = false;
    }

    public void updateState(float blend) {
        this.blend = blend;
        this.tintStrength = blend * (0.24F + blend * 0.18F);
        this.desaturationStrength = 0.0F;
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
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        ensurePostChain(minecraft);
        if (postChain == null) {
            return;
        }

        resizeIfNeeded(minecraft);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        time += partialTick / 20.0F;

        postChain.setUniform("time", time);
        postChain.setUniform("Blend", blend);
        postChain.setUniform("TintStrength", tintStrength);
        postChain.setUniform("DesaturationStrength", desaturationStrength);
        postChain.setUniform("PulseStrength", pulseStrength);
        postChain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private void ensurePostChain(Minecraft minecraft) {
        if (postChain != null || loadFailed) {
            return;
        }

        try {
            postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    POST_CHAIN_LOCATION);
            cachedWidth = minecraft.getWindow().getWidth();
            cachedHeight = minecraft.getWindow().getHeight();
            postChain.resize(cachedWidth, cachedHeight);
        } catch (IOException | JsonParseException exception) {
            Lodged.LOGGER.error("Failed to load Lodged dizziness post-processing shader", exception);
            close();
            loadFailed = true;
        }
    }

    private void resizeIfNeeded(Minecraft minecraft) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width == cachedWidth && height == cachedHeight) {
            return;
        }

        cachedWidth = width;
        cachedHeight = height;
        postChain.resize(width, height);
    }

    private void close() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }
}
