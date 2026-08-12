package com.jvn.lodged.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Sync;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class LodgedConfigSyncTest {
    private static final List<String> SERVER_CONTROLLED_GROUPS = List.of(
            "projectileCollision",
            "arrowRecovery",
            "arrowBreakage",
            "playerArrowRemoval",
            "tamedMobArrowRemoval",
            "arrowDepth",
            "shieldArrows",
            "armorArrows",
            "legShotSlowness",
            "bleedingRules",
            "bleedingEffect");

    private static final List<FieldRef> CLIENT_PRESENTATION_OPTIONS = List.of(
            new FieldRef(LodgedConfigModel.ProjectileCollision.class, "showModelHitboxesInDebug"),
            new FieldRef(LodgedConfigModel.PlayerArrowRemoval.class, "showInventoryTurnHint"),
            new FieldRef(LodgedConfigModel.PlayerArrowRemoval.class, "showInventoryTurnHintTooltip"),
            new FieldRef(LodgedConfigModel.PlayerArrowRemoval.class, "enableArrowRemovalAnimation"),
            new FieldRef(LodgedConfigModel.PlayerArrowRemoval.class, "arrowRemovalAnimationSpeedMultiplier"),
            new FieldRef(LodgedConfigModel.ArrowDepth.class, "shallowRemovalAnimationMs"),
            new FieldRef(LodgedConfigModel.ArrowDepth.class, "lodgedRemovalAnimationMs"),
            new FieldRef(LodgedConfigModel.ArrowDepth.class, "deepLodgedRemovalAnimationMs"),
            new FieldRef(LodgedConfigModel.ShieldArrows.class, "renderOwnShieldArrowsInFirstPerson"),
            new FieldRef(LodgedConfigModel.ArmorArrows.class, "renderArmorArrows"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "bleedingDripParticles"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodCompatParticles"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalChance"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalScaleMultiplier"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalMinScale"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalMaxScale"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalRandomMinScale"),
            new FieldRef(LodgedConfigModel.BleedingEffect.class, "simpleBloodGroundDecalRandomMaxScale"));

    @Test
    void gameplayGroupsOverrideClientValues() throws ReflectiveOperationException {
        for (String fieldName : SERVER_CONTROLLED_GROUPS) {
            assertSyncMode(
                    LodgedConfigModel.class.getDeclaredField(fieldName),
                    Option.SyncMode.OVERRIDE_CLIENT);
        }
    }

    @Test
    void presentationOptionsRemainClientControlled() throws ReflectiveOperationException {
        for (FieldRef fieldRef : CLIENT_PRESENTATION_OPTIONS) {
            assertSyncMode(
                    fieldRef.owner().getDeclaredField(fieldRef.name()),
                    Option.SyncMode.NONE);
        }
    }

    private static void assertSyncMode(Field field, Option.SyncMode expected) {
        Sync sync = field.getAnnotation(Sync.class);
        assertNotNull(sync, () -> field + " must declare an explicit sync mode");
        assertEquals(expected, sync.value(), () -> "Unexpected sync mode for " + field);
    }

    private record FieldRef(Class<?> owner, String name) {
    }
}
