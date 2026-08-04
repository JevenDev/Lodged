package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UseBandagePayload(Action action) implements CustomPacketPayload {
    public static final Type<UseBandagePayload> TYPE =
            new Type<>(LodgedNetwork.id("use_bandage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseBandagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.idMapper(Action::byId, Action::id),
                    UseBandagePayload::action,
                    UseBandagePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        START(0),
        CANCEL(1);

        private final int id;

        Action(int id) {
            this.id = id;
        }

        private int id() {
            return id;
        }

        private static Action byId(int id) {
            for (Action action : values()) {
                if (action.id == id) {
                    return action;
                }
            }
            return CANCEL;
        }
    }
}
