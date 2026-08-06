package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemoveHorseArmorArrowPayload(int horseEntityId, int arrowIndex, Target target) implements CustomPacketPayload {
    public static final Type<RemoveHorseArmorArrowPayload> TYPE =
            new Type<>(LodgedNetwork.id("remove_horse_armor_arrow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveHorseArmorArrowPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RemoveHorseArmorArrowPayload::horseEntityId,
                    ByteBufCodecs.VAR_INT,
                    RemoveHorseArmorArrowPayload::arrowIndex,
                    ByteBufCodecs.idMapper(Target::byId, Target::id),
                    RemoveHorseArmorArrowPayload::target,
                    RemoveHorseArmorArrowPayload::new);

    public RemoveHorseArmorArrowPayload(int horseEntityId, int arrowIndex) {
        this(horseEntityId, arrowIndex, Target.ARMOR);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
