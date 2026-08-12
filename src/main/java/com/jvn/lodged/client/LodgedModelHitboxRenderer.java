package com.jvn.lodged.client;

import com.jvn.lodged.collision.ModelColliderPart;
import com.jvn.lodged.collision.ModelHitboxCache;
import com.jvn.lodged.collision.ModelHitboxCacheHolder;
import com.jvn.lodged.config.LodgedConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class LodgedModelHitboxRenderer {
    private static final float RED = 0.15F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.2F;
    private static final Vector3f TRANSFORMED_POSITION = new Vector3f();
    private static final Vector3f TRANSFORMED_NORMAL = new Vector3f();

    private LodgedModelHitboxRenderer() {
    }

    public static void render(
            LivingEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers) {
        if (!(entity instanceof ModelHitboxCacheHolder holder)) {
            return;
        }

        ModelHitboxCache cache = holder.lodged$getModelHitboxCache();
        if (!cache.prepare(entity, partialTick)) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.lines());
        double inflation = LodgedConfig.modelHitboxInflation();
        double scale = cache.scale();
        for (int partIndex = 0; partIndex < cache.partCount(); partIndex++) {
            ModelColliderPart part = cache.part(partIndex);
            double minX = part.minX() * scale - inflation;
            double minY = part.minY() * scale - inflation;
            double minZ = part.minZ() * scale - inflation;
            double maxX = part.maxX() * scale + inflation;
            double maxY = part.maxY() * scale + inflation;
            double maxZ = part.maxZ() * scale + inflation;

            edge(cache, partIndex, poseStack, buffer, minX, minY, minZ, maxX, minY, minZ);
            edge(cache, partIndex, poseStack, buffer, maxX, minY, minZ, maxX, maxY, minZ);
            edge(cache, partIndex, poseStack, buffer, maxX, maxY, minZ, minX, maxY, minZ);
            edge(cache, partIndex, poseStack, buffer, minX, maxY, minZ, minX, minY, minZ);
            edge(cache, partIndex, poseStack, buffer, minX, minY, maxZ, maxX, minY, maxZ);
            edge(cache, partIndex, poseStack, buffer, maxX, minY, maxZ, maxX, maxY, maxZ);
            edge(cache, partIndex, poseStack, buffer, maxX, maxY, maxZ, minX, maxY, maxZ);
            edge(cache, partIndex, poseStack, buffer, minX, maxY, maxZ, minX, minY, maxZ);
            edge(cache, partIndex, poseStack, buffer, minX, minY, minZ, minX, minY, maxZ);
            edge(cache, partIndex, poseStack, buffer, maxX, minY, minZ, maxX, minY, maxZ);
            edge(cache, partIndex, poseStack, buffer, maxX, maxY, minZ, maxX, maxY, maxZ);
            edge(cache, partIndex, poseStack, buffer, minX, maxY, minZ, minX, maxY, maxZ);
        }
    }

    private static void edge(
            ModelHitboxCache cache,
            int partIndex,
            PoseStack poseStack,
            VertexConsumer buffer,
            double fromLocalX,
            double fromLocalY,
            double fromLocalZ,
            double toLocalX,
            double toLocalY,
            double toLocalZ) {
        double fromX = worldX(cache, partIndex, fromLocalX, fromLocalY, fromLocalZ);
        double fromY = worldY(cache, partIndex, fromLocalX, fromLocalY, fromLocalZ);
        double fromZ = worldZ(cache, partIndex, fromLocalX, fromLocalY, fromLocalZ);
        double toX = worldX(cache, partIndex, toLocalX, toLocalY, toLocalZ);
        double toY = worldY(cache, partIndex, toLocalX, toLocalY, toLocalZ);
        double toZ = worldZ(cache, partIndex, toLocalX, toLocalY, toLocalZ);

        PoseStack.Pose pose = poseStack.last();
        pose.transformNormal(
                (float) (toX - fromX),
                (float) (toY - fromY),
                (float) (toZ - fromZ),
                TRANSFORMED_NORMAL);
        TRANSFORMED_NORMAL.normalize();
        pose.pose().transformPosition((float) fromX, (float) fromY, (float) fromZ, TRANSFORMED_POSITION);
        vertex(buffer, TRANSFORMED_POSITION, TRANSFORMED_NORMAL);
        pose.pose().transformPosition((float) toX, (float) toY, (float) toZ, TRANSFORMED_POSITION);
        vertex(buffer, TRANSFORMED_POSITION, TRANSFORMED_NORMAL);
    }

    private static double worldX(
            ModelHitboxCache cache,
            int partIndex,
            double localX,
            double localY,
            double localZ) {
        return cache.originX(partIndex)
                + cache.axisXX(partIndex) * localX
                + cache.axisYX(partIndex) * localY
                + cache.axisZX(partIndex) * localZ;
    }

    private static double worldY(
            ModelHitboxCache cache,
            int partIndex,
            double localX,
            double localY,
            double localZ) {
        return cache.originY(partIndex)
                + cache.axisXY(partIndex) * localX
                + cache.axisYY(partIndex) * localY
                + cache.axisZY(partIndex) * localZ;
    }

    private static double worldZ(
            ModelHitboxCache cache,
            int partIndex,
            double localX,
            double localY,
            double localZ) {
        return cache.originZ(partIndex)
                + cache.axisXZ(partIndex) * localX
                + cache.axisYZ(partIndex) * localY
                + cache.axisZZ(partIndex) * localZ;
    }

    private static void vertex(VertexConsumer buffer, Vector3f position, Vector3f normal) {
        buffer.addVertex(position.x(), position.y(), position.z())
                .setColor(RED, GREEN, BLUE, 1.0F)
                .setNormal(normal.x(), normal.y(), normal.z());
    }
}
