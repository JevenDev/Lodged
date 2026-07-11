package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

public record ArrowRemovalResultPayload(
        Result result,
        RemovePlayerArrowPayload.Target target,
        InteractionHand hand,
        LodgedArrowVisual arrow,
        int inventorySlot) implements CustomPacketPayload {
    public static final Type<ArrowRemovalResultPayload> TYPE = new Type<>(LodgedNetwork.id("arrow_removal_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArrowRemovalResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Result::byId, Result::id),
            ArrowRemovalResultPayload::result,
            ByteBufCodecs.idMapper(RemovePlayerArrowPayload.Target::byId, RemovePlayerArrowPayload.Target::id),
            ArrowRemovalResultPayload::target,
            ByteBufCodecs.idMapper(ArrowRemovalResultPayload::handById, InteractionHand::ordinal),
            ArrowRemovalResultPayload::hand,
            LodgedArrowVisual.STREAM_CODEC,
            ArrowRemovalResultPayload::arrow,
            ByteBufCodecs.VAR_INT,
            ArrowRemovalResultPayload::inventorySlot,
            ArrowRemovalResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static InteractionHand handById(int id) {
        InteractionHand[] values = InteractionHand.values();
        return id >= 0 && id < values.length ? values[id] : InteractionHand.MAIN_HAND;
    }

    public enum Result {
        SUCCESS(0),
        FAILED(1),
        TOO_RISKY(2),
        INVENTORY_FULL(3),
        CANT_REMOVE_NOW(4),
        REMOVED(5);

        private final int id;

        Result(int id) {
            this.id = id;
        }

        private int id() {
            return id;
        }

        private static Result byId(int id) {
            for (Result result : values()) {
                if (result.id == id) {
                    return result;
                }
            }
            return CANT_REMOVE_NOW;
        }
    }
}
