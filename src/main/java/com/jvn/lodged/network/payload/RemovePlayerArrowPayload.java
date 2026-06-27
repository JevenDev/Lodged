package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemovePlayerArrowPayload(int arrowIndex, Target target, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<RemovePlayerArrowPayload> TYPE = new Type<>(LodgedNetwork.id("remove_player_arrow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemovePlayerArrowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RemovePlayerArrowPayload::arrowIndex,
            ByteBufCodecs.idMapper(Target::byId, Target::id),
            RemovePlayerArrowPayload::target,
            ByteBufCodecs.idMapper(RemovePlayerArrowPayload::handById, InteractionHand::ordinal),
            RemovePlayerArrowPayload::hand,
            RemovePlayerArrowPayload::new);

    public RemovePlayerArrowPayload(int arrowIndex) {
        this(arrowIndex, Target.BODY, InteractionHand.MAIN_HAND);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static InteractionHand handById(int id) {
        InteractionHand[] values = InteractionHand.values();
        return id >= 0 && id < values.length ? values[id] : InteractionHand.MAIN_HAND;
    }

    public enum Target {
        BODY(0),
        SHIELD(1);

        private final int id;

        Target(int id) {
            this.id = id;
        }

        int id() {
            return id;
        }

        static Target byId(int id) {
            for (Target target : values()) {
                if (target.id == id) {
                    return target;
                }
            }
            return BODY;
        }
    }
}
