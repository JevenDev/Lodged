package com.jvn.lodged.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BandageAnimationTest {
    @Test
    void armsAndWrappingSettleAtBothEnds() {
        for (float progress : new float[]{-0.1F, 0.0F, 1.0F, 1.1F}) {
            TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(progress);

            assertEquals(0.0F, motion.amount(), 0.00001F);
            assertEquals(0.0F, motion.supportAmount(), 0.00001F);
            assertEquals(0.0F, motion.stroke(), 0.00001F);
            assertEquals(0.0F, motion.orbit(), 0.00001F);
            assertEquals(0.0F, motion.press(), 0.00001F);
        }
    }

    @Test
    void supportsTheArmBeforeWrappingAndPressesAfterTheWraps() {
        TreatmentAnimation.BandageMotion entry = TreatmentAnimation.bandage(0.12F);
        TreatmentAnimation.BandageMotion wrapping = TreatmentAnimation.bandage(0.4F);
        TreatmentAnimation.BandageMotion finishing = TreatmentAnimation.bandage(0.8F);

        assertTrue(entry.supportAmount() > entry.amount());
        assertEquals(0.0F, entry.stroke());
        assertEquals(0.0F, entry.orbit());
        assertEquals(1.0F, wrapping.supportAmount(), 0.00001F);
        assertEquals(0.0F, wrapping.press(), 0.00001F);
        assertEquals(0.0F, finishing.stroke(), 0.00001F);
        assertEquals(0.0F, finishing.orbit(), 0.00001F);
        assertTrue(finishing.press() > 0.9F);
    }

    @Test
    void bothArmsStayFullyRaisedThroughoutTheWrappingPasses() {
        for (float progress = 0.18F; progress <= 0.72F; progress += 0.005F) {
            TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(progress);

            assertEquals(1.0F, motion.amount(), 0.00001F);
            assertEquals(1.0F, motion.supportAmount(), 0.00001F);
            assertTrue(Math.abs(motion.stroke()) <= 1.0F);
            assertTrue(Math.abs(motion.orbit()) <= 1.0F);
        }
    }

    @Test
    void firstPersonArmsKeepTheirWrappingOrientationDuringEntryAndExit() {
        for (boolean holdingBandage : new boolean[]{false, true}) {
            for (boolean rightArm : new boolean[]{false, true}) {
                TreatmentAnimation.BandageArmPose raised = TreatmentAnimation.bandageArm(
                        TreatmentAnimation.bandage(0.18F), holdingBandage, rightArm, 0.0F);
                for (float progress : new float[]{0.0F, 0.07F, 0.14F, 0.18F, 0.9F, 0.95F, 1.0F}) {
                    TreatmentAnimation.BandageArmPose pose = TreatmentAnimation.bandageArm(
                            TreatmentAnimation.bandage(progress), holdingBandage, rightArm, 0.0F);

                    assertEquals(raised.xRot(), pose.xRot(), 0.00001F);
                    assertEquals(raised.yRot(), pose.yRot(), 0.00001F);
                    assertEquals(raised.zRot(), pose.zRot(), 0.00001F);
                }
            }
        }
    }

    @Test
    void firstPersonArmsRiseFromBelowTheViewAndLowerBeforeFinishing() {
        for (boolean holdingBandage : new boolean[]{false, true}) {
            TreatmentAnimation.BandageArmPose lowered = TreatmentAnimation.bandageArm(
                    TreatmentAnimation.bandage(0.0F), holdingBandage, true, 0.0F);
            TreatmentAnimation.BandageArmPose entering = TreatmentAnimation.bandageArm(
                    TreatmentAnimation.bandage(0.07F), holdingBandage, true, 0.0F);
            TreatmentAnimation.BandageArmPose raised = TreatmentAnimation.bandageArm(
                    TreatmentAnimation.bandage(0.18F), holdingBandage, true, 0.0F);
            TreatmentAnimation.BandageArmPose leaving = TreatmentAnimation.bandageArm(
                    TreatmentAnimation.bandage(0.95F), holdingBandage, true, 0.0F);
            TreatmentAnimation.BandageArmPose finished = TreatmentAnimation.bandageArm(
                    TreatmentAnimation.bandage(1.0F), holdingBandage, true, 0.0F);

            assertTrue(lowered.y() < entering.y() && entering.y() < raised.y());
            assertTrue(lowered.y() < leaving.y() && leaving.y() < raised.y());
            assertTrue(raised.y() - lowered.y() >= 1.0F);
            assertEquals(lowered, finished);
        }
    }

    @Test
    void firstPersonPosesMirrorAcrossHandsThroughoutBandaging() {
        for (boolean holdingBandage : new boolean[]{false, true}) {
            for (float progress = 0.0F; progress <= 1.0F; progress += 0.01F) {
                TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(progress);
                TreatmentAnimation.BandageArmPose right =
                        TreatmentAnimation.bandageArm(motion, holdingBandage, true, 0.0F);
                TreatmentAnimation.BandageArmPose left =
                        TreatmentAnimation.bandageArm(motion, holdingBandage, false, 0.0F);

                assertEquals(right.x(), -left.x());
                assertEquals(right.y(), left.y());
                assertEquals(right.z(), left.z());
                assertEquals(right.xRot(), left.xRot());
                assertEquals(right.yRot(), -left.yRot());
                assertEquals(right.zRot(), -left.zRot());
            }
        }
    }

    @Test
    void equipChangesDoNotDisplaceTheArmsWhileWrapping() {
        for (boolean holdingBandage : new boolean[]{false, true}) {
            for (float progress = 0.18F; progress <= 0.72F; progress += 0.01F) {
                TreatmentAnimation.BandageMotion motion = TreatmentAnimation.bandage(progress);
                assertEquals(
                        TreatmentAnimation.bandageArm(motion, holdingBandage, true, 0.0F),
                        TreatmentAnimation.bandageArm(motion, holdingBandage, true, 1.0F));
            }
        }
    }

    @Test
    void phaseChangesDoNotSnapTheHands() {
        for (float boundary : new float[]{0.14F, 0.18F, 0.72F, 0.84F, 0.88F, 1.0F}) {
            TreatmentAnimation.BandageMotion before = TreatmentAnimation.bandage(boundary - 0.00001F);
            TreatmentAnimation.BandageMotion after = TreatmentAnimation.bandage(boundary + 0.00001F);

            assertEquals(before.amount(), after.amount(), 0.003F);
            assertEquals(before.supportAmount(), after.supportAmount(), 0.003F);
            assertEquals(before.stroke(), after.stroke(), 0.003F);
            assertEquals(before.orbit(), after.orbit(), 0.003F);
            assertEquals(before.press(), after.press(), 0.003F);
        }
    }
}
