package com.jvn.lodged.network;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.world.LodgedArmorArrowStorage.LodgedArmorArrowData;
import com.jvn.lodged.world.LodgedArrowData;
import com.jvn.lodged.world.LodgedShieldArrowStorage.LodgedShieldArrowData;

final class ArrowRemovalRules {
    private ArrowRemovalRules() {
    }

    static double removalSuccessChance(LodgedArrowData arrow) {
        double baseSuccessChance = LodgedConfig.playerArrowRemovalSuccessChance(arrow.visual().bodyPart())
                * LodgedConfig.arrowDepthRemovalSuccessMultiplier(arrow.visual().depth());
        return removalSuccessChance(arrow.infinityGenerated(), baseSuccessChance);
    }

    static double removalSuccessChance(LodgedShieldArrowData arrow) {
        double successChance = LodgedConfig.shieldArrowRemovalSuccessChance();
        return removalSuccessChance(arrow.infinityGenerated(), successChance);
    }

    static double removalSuccessChance(LodgedArmorArrowData arrow) {
        double successChance = LodgedConfig.armorArrowRemovalSuccessChance();
        return removalSuccessChance(arrow.infinityGenerated(), successChance);
    }

    static double removalSuccessChance(boolean infinityGenerated, double baseSuccessChance) {
        if (infinityGenerated && LodgedConfig.recoverInfinityArrows()) {
            return baseSuccessChance * LodgedConfig.playerArrowRemovalInfinitySuccessMultiplier();
        }
        return baseSuccessChance;
    }

    static boolean canRecoverArrow(LodgedArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobArrows());
    }

    static boolean canRecoverArrow(LodgedShieldArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobShieldArrows());
    }

    static boolean canRecoverArrow(LodgedArmorArrowData arrow) {
        return arrow.recoverable()
                && canRecoverArrow(
                        arrow.fromPlayer(),
                        arrow.infinityGenerated(),
                        arrow.creativeGenerated(),
                        LodgedConfig.recoverMobArmorArrows());
    }

    static boolean canRecoverArrow(
            boolean fromPlayer,
            boolean infinityGenerated,
            boolean creativeGenerated,
            boolean recoverMobArrows) {
        if (!fromPlayer && !recoverMobArrows) {
            return false;
        }

        if (infinityGenerated && !LodgedConfig.recoverInfinityArrows()) {
            return false;
        }

        return !creativeGenerated || LodgedConfig.recoverCreativeArrows();
    }
}
