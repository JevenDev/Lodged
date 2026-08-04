package com.jvn.lodged.client;

import com.jvn.lodged.effect.LodgedEffects;
import com.jvn.lodged.network.ClientBandageState;
import com.jvn.lodged.network.payload.UseBandagePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LodgedBandageClient {
    private static final KeyMapping USE_BANDAGE_KEY = new KeyMapping(
            "key.lodged.use_bandage",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.CATEGORY_GAMEPLAY);
    private static boolean bandageKeyWasDown;

    private LodgedBandageClient() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ((IKeyMappingExtension) USE_BANDAGE_KEY).setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(USE_BANDAGE_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientBandageState.tick();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            bandageKeyWasDown = false;
            return;
        }

        boolean keyDown = USE_BANDAGE_KEY.isDown();
        if (keyDown
                && !bandageKeyWasDown
                && !player.isUsingItem()
                && !ClientBandageState.isActive(player)
                && player.hasEffect(LodgedEffects.BLEEDING)) {
            PacketDistributor.sendToServer(
                    new UseBandagePayload(UseBandagePayload.Action.START));
        } else if (!keyDown && bandageKeyWasDown) {
            PacketDistributor.sendToServer(
                    new UseBandagePayload(UseBandagePayload.Action.CANCEL));
        }
        bandageKeyWasDown = keyDown;
    }

    public static boolean shouldKeepUsingBandage(LocalPlayer player) {
        return USE_BANDAGE_KEY.isDown() && BandageAnimation.isUsingBandage(player);
    }
}
