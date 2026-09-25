package com.jvn.lodged.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrowRemovalCooldownTest {
    @Test
    void acceptsAgainAtTheCooldownBoundary() {
        ArrowRemovalCooldown cooldown = new ArrowRemovalCooldown(2);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.accept(player, 10));
        assertFalse(cooldown.accept(player, 10));
        assertFalse(cooldown.accept(player, 11));
        assertTrue(cooldown.accept(player, 12));
    }

    @Test
    void rejectedRequestsDoNotExtendTheCooldown() {
        ArrowRemovalCooldown cooldown = new ArrowRemovalCooldown(2);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.accept(player, 100));
        assertFalse(cooldown.accept(player, 101));
        assertTrue(cooldown.accept(player, 102));
    }

    @Test
    void clearingAPlayerLeavesOtherPlayersOnCooldown() {
        ArrowRemovalCooldown cooldown = new ArrowRemovalCooldown(2);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(cooldown.accept(first, 10));
        assertTrue(cooldown.accept(second, 10));

        cooldown.clear(first);

        assertTrue(cooldown.accept(first, 10));
        assertFalse(cooldown.accept(second, 10));
    }

    @Test
    void clearingServerStateAllowsRequestsAtANewWorldTime() {
        ArrowRemovalCooldown cooldown = new ArrowRemovalCooldown(2);
        UUID player = UUID.randomUUID();
        assertTrue(cooldown.accept(player, 100));
        assertFalse(cooldown.accept(player, 0));

        cooldown.clear();

        assertTrue(cooldown.accept(player, 0));
    }
}
