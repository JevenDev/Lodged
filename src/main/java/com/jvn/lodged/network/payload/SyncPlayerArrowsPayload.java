package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncPlayerArrowsPayload(int arrowCount) implements CustomPacketPayload {
    public static final Type<SyncPlayerArrowsPayload> TYPE = new Type<>(LodgedNetwork.id("sync_player_arrows"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerArrowsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncPlayerArrowsPayload::arrowCount,
            SyncPlayerArrowsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
