package com.jvn.lodged.network.payload;

import com.jvn.lodged.network.LodgedNetwork;
import com.jvn.lodged.network.payload.RemovePlayerArrowPayload.Target;
import com.jvn.lodged.world.LodgedArrowVisual;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;

public record SyncArmorArrowRemovalPayload(
        int entityId,
        boolean active,
        Target target,
        EquipmentSlot slot,
        LodgedArrowVisual arrow) implements CustomPacketPayload {
    public static final Type<SyncArmorArrowRemovalPayload> TYPE = new Type<>(LodgedNetwork.id("sync_armor_arrow_removal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncArmorArrowRemovalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncArmorArrowRemovalPayload::entityId,
            ByteBufCodecs.BOOL,
            SyncArmorArrowRemovalPayload::active,
            ByteBufCodecs.idMapper(Target::byId, Target::id),
            SyncArmorArrowRemovalPayload::target,
            ByteBufCodecs.idMapper(SyncArmorArrowRemovalPayload::slotById, EquipmentSlot::ordinal),
            SyncArmorArrowRemovalPayload::slot,
            LodgedArrowVisual.STREAM_CODEC,
            SyncArmorArrowRemovalPayload::arrow,
            SyncArmorArrowRemovalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static EquipmentSlot slotById(int id) {
        EquipmentSlot[] values = EquipmentSlot.values();
        return id >= 0 && id < values.length ? values[id] : EquipmentSlot.CHEST;
    }
}
