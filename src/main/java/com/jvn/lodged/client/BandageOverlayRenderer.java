package com.jvn.lodged.client;

import com.jvn.lodged.Lodged;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

public final class BandageOverlayRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "textures/entity/bandage_overlay.png");
    private static final ModelLayerLocation WIDE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "bandage_overlay"), "wide");
    private static final ModelLayerLocation SLIM_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Lodged.MOD_ID, "bandage_overlay"), "slim");
    private static final Map<Model, PlayerModel<AbstractClientPlayer>> FIRST_PERSON_MODELS =
            new EnumMap<>(Model.class);

    private BandageOverlayRenderer() {
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WIDE_LAYER, () -> createLayer(false));
        event.registerLayerDefinition(SLIM_LAYER, () -> createLayer(true));
    }

    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (Model skinModel : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skinModel);
            if (renderer == null) {
                continue;
            }

            boolean slim = skinModel == Model.SLIM;
            ModelLayerLocation layerLocation = slim ? SLIM_LAYER : WIDE_LAYER;
            PlayerModel<AbstractClientPlayer> thirdPersonModel =
                    new PlayerModel<>(event.getEntityModels().bakeLayer(layerLocation), slim);
            renderer.addLayer(new BandageOverlayLayer(renderer, thirdPersonModel));
            FIRST_PERSON_MODELS.put(
                    skinModel,
                    new PlayerModel<>(event.getEntityModels().bakeLayer(layerLocation), slim));
        }
    }

    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (!BandageAnimation.isUsingBandage(player) || player.isInvisible()) {
            return;
        }
        HumanoidArm renderedArm = event.getArm();
        HumanoidArm treatedArm = BandageAnimation.treatedArm(player);

        PlayerModel<AbstractClientPlayer> overlayModel = FIRST_PERSON_MODELS.get(player.getSkin().model());
        if (overlayModel == null
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)
                instanceof PlayerRenderer renderer)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();
        playerModel.attackTime = 0.0F;
        playerModel.crouching = false;
        playerModel.swimAmount = 0.0F;
        playerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        ModelPart playerArm = arm(playerModel, renderedArm);
        playerArm.xRot = renderedArm == treatedArm ? Mth.PI : 0.0F;
        playerArm.yRot = 0.0F;
        playerArm.zRot = renderedArm == treatedArm ? 0.0F : Mth.PI;
        playerArm.visible = true;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        playerArm.render(
                poseStack,
                buffer.getBuffer(RenderType.entitySolid(player.getSkin().texture())),
                packedLight,
                OverlayTexture.NO_OVERLAY);

        ModelPart sleeve = sleeve(playerModel, renderedArm);
        sleeve.copyFrom(playerArm);
        sleeve.visible = player.isModelPartShown(
                renderedArm == HumanoidArm.RIGHT
                        ? PlayerModelPart.RIGHT_SLEEVE
                        : PlayerModelPart.LEFT_SLEEVE);
        sleeve.render(
                poseStack,
                buffer.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                packedLight,
                OverlayTexture.NO_OVERLAY);

        if (renderedArm == treatedArm) {
            renderBandageLayers(
                    overlayModel,
                    treatedArm,
                    playerArm,
                    poseStack,
                    buffer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY);
        }
        event.setCanceled(true);
    }

    private static LayerDefinition createLayer(boolean slim) {
        return LayerDefinition.create(
                PlayerModel.createMesh(new CubeDeformation(0.04F), slim),
                64,
                64);
    }

    private static ModelPart arm(PlayerModel<?> model, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    private static ModelPart sleeve(PlayerModel<?> model, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
    }

    private static void renderBandageLayers(
            PlayerModel<?> overlayModel,
            HumanoidArm treatedArm,
            ModelPart sourceArm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ModelPart overlayArm = arm(overlayModel, treatedArm);
        overlayArm.copyFrom(sourceArm);
        overlayArm.visible = true;
        overlayArm.render(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight,
                packedOverlay);

        ModelPart overlaySleeve = sleeve(overlayModel, treatedArm);
        overlaySleeve.copyFrom(sourceArm);
        overlaySleeve.visible = true;
        overlaySleeve.render(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight,
                packedOverlay);
    }

    private static final class BandageOverlayLayer
            extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final PlayerModel<AbstractClientPlayer> overlayModel;

        private BandageOverlayLayer(
                PlayerRenderer renderer,
                PlayerModel<AbstractClientPlayer> overlayModel) {
            super(renderer);
            this.overlayModel = overlayModel;
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player,
                float limbSwing,
                float limbSwingAmount,
                float partialTicks,
                float ageInTicks,
                float netHeadYaw,
                float headPitch) {
            HumanoidArm treatedArm = BandageAnimation.treatedArm(player);
            if (treatedArm == null || player.isInvisible()) {
                return;
            }

            renderBandageLayers(
                    overlayModel,
                    treatedArm,
                    arm(getParentModel(), treatedArm),
                    poseStack,
                    buffer,
                    packedLight,
                    LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        }
    }
}
