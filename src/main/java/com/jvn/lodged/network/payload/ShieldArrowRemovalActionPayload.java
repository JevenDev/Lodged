package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

public record ShieldArrowRemovalActionPayload(Action action, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ShieldArrowRemovalActionPayload> TYPE = new Type<>(LodgedNetwork.id("shield_arrow_removal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldArrowRemovalActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Action::byId, Action::id),
            ShieldArrowRemovalActionPayload::action,
            ByteBufCodecs.idMapper(ShieldArrowRemovalActionPayload::handById, InteractionHand::ordinal),
            ShieldArrowRemovalActionPayload::hand,
            ShieldArrowRemovalActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static InteractionHand handById(int id) {
        InteractionHand[] values = InteractionHand.values();
        return id >= 0 && id < values.length ? values[id] : InteractionHand.MAIN_HAND;
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
