package com.jvn.lodged.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LodgedArrowCountTest {
    @Test
    void removingTheLastTrackedArrowClearsItsVanillaCount() {
        assertEquals(0, LodgedArrowCount.afterRemoval(1, 1, 0));
    }

    @Test
    void evictingAtCapacityOffsetsTheNewVanillaArrow() {
        assertEquals(8, LodgedArrowCount.afterRemoval(9, 1, 8));
    }

    @Test
    void loweringTheCapCanEvictSeveralArrowsAtOnce() {
        assertEquals(3, LodgedArrowCount.afterRemoval(9, 6, 3));
    }

    @Test
    void removalPreservesUntrackedVanillaArrows() {
        assertEquals(4, LodgedArrowCount.afterRemoval(5, 1, 2));
        assertEquals(2, LodgedArrowCount.afterRemoval(5, 3, 0));
    }

    @Test
    void naturalDespawnCannotProduceNegativeOrUndercountedArrows() {
        assertEquals(2, LodgedArrowCount.afterRemoval(1, 1, 2));
        assertEquals(0, LodgedArrowCount.afterRemoval(0, 3, 0));
    }
}
