package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemovePlayerArrowPayload(int arrowIndex) implements CustomPacketPayload {
    public static final Type<RemovePlayerArrowPayload> TYPE = new Type<>(LodgedNetwork.id("remove_player_arrow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemovePlayerArrowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RemovePlayerArrowPayload::arrowIndex,
            RemovePlayerArrowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
