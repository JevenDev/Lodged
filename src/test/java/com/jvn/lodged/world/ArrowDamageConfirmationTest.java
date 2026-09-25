package com.jvn.lodged.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrowDamageConfirmationTest {
    @Test
    void missingOrFullyPreventedDamageCannotConfirmAnImpact() {
        UUID target = UUID.randomUUID();
        ArrowDamageConfirmation confirmation = new ArrowDamageConfirmation(target);

        assertFalse(confirmation.isConfirmed());
        confirmation.recordDamage(target, 0.0F, 0.0F);
        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void damageToAnotherTargetCannotConfirmTheImpact() {
        ArrowDamageConfirmation confirmation = new ArrowDamageConfirmation(UUID.randomUUID());

        confirmation.recordDamage(UUID.randomUUID(), 4.0F, 2.0F);

        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void partialBlocksStillConfirmWhenHealthIsLost() {
        UUID target = UUID.randomUUID();
        ArrowDamageConfirmation confirmation = new ArrowDamageConfirmation(target);

        confirmation.recordDamage(target, 0.5F, 0.0F);

        assertTrue(confirmation.isConfirmed());
    }

    @Test
    void absorptionHeartsCountAsDamageWithoutHealthLoss() {
        UUID target = UUID.randomUUID();
        ArrowDamageConfirmation confirmation = new ArrowDamageConfirmation(target);

        confirmation.recordDamage(target, 0.0F, 2.0F);

        assertTrue(confirmation.isConfirmed());
    }

    @Test
    void invalidDamageDoesNotConfirmAnImpact() {
        UUID target = UUID.randomUUID();
        ArrowDamageConfirmation confirmation = new ArrowDamageConfirmation(target);

        confirmation.recordDamage(target, Float.NaN, Float.POSITIVE_INFINITY);
        confirmation.recordDamage(target, -2.0F, -1.0F);

        assertFalse(confirmation.isConfirmed());
    }
}
