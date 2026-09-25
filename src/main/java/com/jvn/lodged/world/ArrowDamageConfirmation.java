package com.jvn.lodged.world;

import java.util.UUID;

public final class ArrowDamageConfirmation {
    private final UUID targetUuid;
    private boolean confirmed;

    public ArrowDamageConfirmation(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public void recordDamage(UUID targetUuid, float healthDamage, float absorbedDamage) {
        if (this.targetUuid.equals(targetUuid)
                && (isPositiveDamage(healthDamage) || isPositiveDamage(absorbedDamage))) {
            confirmed = true;
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private static boolean isPositiveDamage(float damage) {
        return Float.isFinite(damage) && damage > 0.0F;
    }
}
