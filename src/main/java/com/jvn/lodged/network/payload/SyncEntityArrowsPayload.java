package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.world.LodgedArrowVisual;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncEntityArrowsPayload(int entityId, List<LodgedArrowVisual> arrows, int arrowCount) implements CustomPacketPayload {
    public static final Type<SyncEntityArrowsPayload> TYPE = new Type<>(LodgedNetwork.id("sync_entity_arrows"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntityArrowsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncEntityArrowsPayload::entityId,
            LodgedArrowVisual.STREAM_CODEC.apply(ByteBufCodecs.list(64)),
            SyncEntityArrowsPayload::arrows,
            ByteBufCodecs.INT,
            SyncEntityArrowsPayload::arrowCount,
            SyncEntityArrowsPayload::new);

    public SyncEntityArrowsPayload {
        arrows = List.copyOf(Objects.requireNonNull(arrows));
        arrowCount = Math.max(0, arrowCount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
