package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncPlayerArrowsPayload(List<LodgedArrowVisual> arrows) implements CustomPacketPayload {
    public static final Type<SyncPlayerArrowsPayload> TYPE = new Type<>(LodgedNetwork.id("sync_player_arrows"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerArrowsPayload> STREAM_CODEC = StreamCodec.composite(
            LodgedArrowVisual.STREAM_CODEC.apply(ByteBufCodecs.list(64)),
            SyncPlayerArrowsPayload::arrows,
            SyncPlayerArrowsPayload::new);

    public SyncPlayerArrowsPayload {
        arrows = List.copyOf(Objects.requireNonNull(arrows));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
