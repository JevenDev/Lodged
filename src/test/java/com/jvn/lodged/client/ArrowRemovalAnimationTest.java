package com.jvn.lodged.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArrowRemovalAnimationTest {
    @Test
    void lowersTheHeldItemBeforeReachingAndGripping() {
        float delay = TreatmentAnimation.ITEM_DROP_TICKS;
        TreatmentAnimation.RemovalMotion waiting = TreatmentAnimation.removal(delay, true, 1.0F);

        assertEquals(0.0F, waiting.reach());
        assertEquals(0.0F, waiting.grip());
        assertEquals(0.0F, waiting.pull());
        assertEquals(1.0F, TreatmentAnimation.itemDrop(delay));
        for (float ticks = 0.0F; ticks < 80.0F; ticks += 0.25F) {
            TreatmentAnimation.RemovalMotion emptyHand = TreatmentAnimation.removal(ticks, false, 1.0F);
            TreatmentAnimation.RemovalMotion heldItem = TreatmentAnimation.removal(ticks + delay, true, 1.0F);
            assertEquals(emptyHand.reach(), heldItem.reach(), 0.00001F);
            assertEquals(emptyHand.grip(), heldItem.grip(), 0.00001F);
            assertEquals(emptyHand.pull(), heldItem.pull(), 0.00001F);
            assertEquals(emptyHand.resistance(), heldItem.resistance(), 0.00001F);
        }
    }

    @Test
    void interpolatesBetweenTicksWithoutAdvancingForModelOnlyRenders() {
        assertEquals(6.0F, TreatmentAnimation.interpolatedTicks(6, 100, 0.0F));
        assertEquals(6.25F, TreatmentAnimation.interpolatedTicks(6, 100, 100.25F));
        assertEquals(7.0F, TreatmentAnimation.interpolatedTicks(6, 100, 102.0F));
        float first = TreatmentAnimation.removal(3.0F, false, 1.0F).reach();
        float middle = TreatmentAnimation.removal(3.5F, false, 1.0F).reach();
        float last = TreatmentAnimation.removal(4.0F, false, 1.0F).reach();

        assertTrue(first < middle && middle < last);
    }

    @Test
    void longAttemptsKeepTuggingWithinReach() {
        float min = Float.MAX_VALUE;
        float max = 0.0F;
        for (float ticks = 80.0F; ticks < 1200.0F; ticks += 0.25F) {
            TreatmentAnimation.RemovalMotion motion = TreatmentAnimation.removal(ticks, false, 1.55F);
            assertTrue(motion.pull() >= 0.0F && motion.pull() < 0.7F);
            assertTrue(Float.isFinite(motion.strain()));
            min = Math.min(min, motion.pull());
            max = Math.max(max, motion.pull());
        }

        assertTrue(max - min > 0.25F);
    }

    @Test
    void deeperArrowsIncreaseResistanceWithoutChangingTheReachTiming() {
        for (float ticks = 1.0F; ticks < 90.0F; ticks += 0.25F) {
            TreatmentAnimation.RemovalMotion shallow = TreatmentAnimation.removal(ticks, false, 0.6F);
            TreatmentAnimation.RemovalMotion deep = TreatmentAnimation.removal(ticks, false, 1.55F);

            assertEquals(shallow.reach(), deep.reach());
            assertEquals(shallow.grip(), deep.grip());
            assertEquals(shallow.pull(), deep.pull());
            assertTrue(deep.resistance() >= shallow.resistance());
        }
    }

    @Test
    void reachingGrippingAndTuggingJoinWithoutJumps() {
        for (float boundary : new float[]{0.0F, 7.0F, 10.0F, 22.0F, 28.0F, 46.0F}) {
            TreatmentAnimation.RemovalMotion before = TreatmentAnimation.removal(boundary - 0.001F, false, 1.0F);
            TreatmentAnimation.RemovalMotion after = TreatmentAnimation.removal(boundary + 0.001F, false, 1.0F);

            assertEquals(before.reach(), after.reach(), 0.002F);
            assertEquals(before.grip(), after.grip(), 0.002F);
            assertEquals(before.pull(), after.pull(), 0.002F);
            assertEquals(before.resistance(), after.resistance(), 0.002F);
        }
    }
}
