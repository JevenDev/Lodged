package com.jvn.lodged.collision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ModelHitboxPartTest {
    @Test
    void identityRoundTripsAndMapsToRendererNames() {
        for (ModelHitboxPart part : ModelHitboxPart.values()) {
            assertSame(part, ModelHitboxPart.byId(part.id()));
            assertSame(part, ModelHitboxPart.bySerializedName(part.serializedName()));
        }

        assertEquals("right_front_leg", ModelHitboxPart.SPIDER_RIGHT_FRONT_LEG.modelPartPath());
        assertEquals("body1", ModelHitboxPart.SPIDER_REAR_BODY.modelPartPath());
    }
}
