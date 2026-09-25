package com.jvn.lodged.world;

final class LodgedArrowCount {
    private LodgedArrowCount() {
    }

    static int afterRemoval(int vanillaArrows, int removedArrows, int storedArrows) {
        // preserve untracked arrows while accounting for vanilla despawn
        return Math.max(storedArrows, vanillaArrows - removedArrows);
    }
}
