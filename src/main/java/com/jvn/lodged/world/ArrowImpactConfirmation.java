package com.jvn.lodged.world;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

final class ArrowImpactConfirmation {
    private ArrowImpactConfirmation() {
    }

    static <T> T consume(
            Map<UUID, T> pendingImpacts,
            UUID arrowUuid,
            UUID targetUuid,
            boolean damageAccepted,
            Function<T, UUID> targetUuidExtractor) {
        T impact = pendingImpacts.remove(arrowUuid);
        if (!damageAccepted
                || impact == null
                || !targetUuid.equals(targetUuidExtractor.apply(impact))) {
            return null;
        }
        return impact;
    }

    static boolean hasVanillaBodyArrow(boolean projectileRemoved, int pierceLevel) {
        return projectileRemoved && pierceLevel <= 0;
    }
}
