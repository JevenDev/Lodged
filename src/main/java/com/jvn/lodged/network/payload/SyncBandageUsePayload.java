package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

public record SyncBandageUsePayload(int entityId, boolean active, InteractionHand hand)
        implements CustomPacketPayload {
    public static final Type<SyncBandageUsePayload> TYPE =
            new Type<>(LodgedNetwork.id("sync_bandage_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBandageUsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SyncBandageUsePayload::entityId,
                    ByteBufCodecs.BOOL,
                    SyncBandageUsePayload::active,
                    ByteBufCodecs.idMapper(SyncBandageUsePayload::handById, InteractionHand::ordinal),
                    SyncBandageUsePayload::hand,
                    SyncBandageUsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static InteractionHand handById(int id) {
        InteractionHand[] values = InteractionHand.values();
        return id >= 0 && id < values.length ? values[id] : InteractionHand.MAIN_HAND;
    }
}
