package com.jvn.lodged.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrowImpactConfirmationTest {
    @Test
    void rejectedDamageConsumesPendingImpactWithoutConfirmingIt() {
        UUID arrowUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        Map<UUID, PendingImpact> impacts = new HashMap<>();
        impacts.put(arrowUuid, new PendingImpact(targetUuid, "body"));

        PendingImpact result = ArrowImpactConfirmation.consume(
                impacts,
                arrowUuid,
                targetUuid,
                false,
                PendingImpact::targetUuid);

        assertNull(result);
        assertTrue(impacts.isEmpty());
    }

    @Test
    void mismatchedTargetCannotConsumeAsAConfirmedHit() {
        UUID arrowUuid = UUID.randomUUID();
        Map<UUID, PendingImpact> impacts = new HashMap<>();
        PendingImpact pending = new PendingImpact(UUID.randomUUID(), "armor");
        impacts.put(arrowUuid, pending);

        PendingImpact result = ArrowImpactConfirmation.consume(
                impacts,
                arrowUuid,
                UUID.randomUUID(),
                true,
                PendingImpact::targetUuid);

        assertNull(result);
        assertTrue(impacts.isEmpty());
    }

    @Test
    void acceptedDamageForTheRememberedTargetConfirmsTheImpact() {
        UUID arrowUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        Map<UUID, PendingImpact> impacts = new HashMap<>();
        PendingImpact pending = new PendingImpact(targetUuid, "body");
        impacts.put(arrowUuid, pending);

        PendingImpact result = ArrowImpactConfirmation.consume(
                impacts,
                arrowUuid,
                targetUuid,
                true,
                PendingImpact::targetUuid);

        assertEquals(pending, result);
        assertTrue(impacts.isEmpty());
    }

    @Test
    void bodyLodgingRequiresVanillaToRemoveANonPiercingArrow() {
        assertTrue(ArrowImpactConfirmation.hasVanillaBodyArrow(true, 0));
        assertFalse(ArrowImpactConfirmation.hasVanillaBodyArrow(false, 0));
        assertFalse(ArrowImpactConfirmation.hasVanillaBodyArrow(true, 1));
    }

    private record PendingImpact(UUID targetUuid, String kind) {
    }
}
