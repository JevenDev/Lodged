package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

public record SyncShieldArrowRemovalPayload(int entityId, boolean active, InteractionHand shieldHand)
        implements CustomPacketPayload {
    public static final Type<SyncShieldArrowRemovalPayload> TYPE = new Type<>(LodgedNetwork.id("sync_shield_arrow_removal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShieldArrowRemovalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncShieldArrowRemovalPayload::entityId,
            ByteBufCodecs.BOOL,
            SyncShieldArrowRemovalPayload::active,
            ByteBufCodecs.idMapper(SyncShieldArrowRemovalPayload::handById, InteractionHand::ordinal),
            SyncShieldArrowRemovalPayload::shieldHand,
            SyncShieldArrowRemovalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static InteractionHand handById(int id) {
        InteractionHand[] values = InteractionHand.values();
        return id >= 0 && id < values.length ? values[id] : InteractionHand.MAIN_HAND;
    }
}
