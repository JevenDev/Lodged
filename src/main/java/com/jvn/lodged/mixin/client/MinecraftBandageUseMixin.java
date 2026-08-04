package com.jvn.lodged.mixin.client;

import com.jvn.lodged.client.LodgedBandageClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftBandageUseMixin {
    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void lodged$keepUsingKeyboundBandage(
            MultiPlayerGameMode gameMode,
            Player player) {
        if (player instanceof LocalPlayer localPlayer
                && LodgedBandageClient.shouldKeepUsingBandage(localPlayer)) {
            return;
        }

        gameMode.releaseUsingItem(player);
    }
}
