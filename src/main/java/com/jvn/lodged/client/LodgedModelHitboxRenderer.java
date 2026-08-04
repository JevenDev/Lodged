package com.jvn.lodged.client;

import com.jvn.lodged.config.LodgedConfig;
import com.jvn.lodged.mixin.client.ModelPartAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Vector3f;

public final class LodgedModelHitboxRenderer {
    private static final float RED = 0.15F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.2F;
    private static final int FLOATS_PER_VERTEX = 6;
    private static final Vector3f TRANSFORMED_POSITION = new Vector3f();
    private static final Vector3f TRANSFORMED_NORMAL = new Vector3f();

    private static float[] vertexData = new float[32_768];
    private static int vertexDataSize;
    private static boolean active;
    private static float inflationPixels;

    private LodgedModelHitboxRenderer() {
    }

    public static void begin() {
        active = true;
        vertexDataSize = 0;
        inflationPixels = (float) (LodgedConfig.modelHitboxInflation() * 16.0D);
    }

    public static void end(MultiBufferSource buffers) {
        if (!active) {
            return;
        }
        active = false;
        VertexConsumer buffer = buffers.getBuffer(RenderType.lines());
        for (int index = 0; index < vertexDataSize; index += FLOATS_PER_VERTEX) {
            buffer.addVertex(vertexData[index], vertexData[index + 1], vertexData[index + 2])
                    .setColor(RED, GREEN, BLUE, 1.0F)
                    .setNormal(vertexData[index + 3], vertexData[index + 4], vertexData[index + 5]);
        }
        vertexDataSize = 0;
    }

    public static void renderPart(PoseStack poseStack, ModelPart part) {
        if (!active || part.skipDraw) {
            return;
        }

        List<ModelPart.Cube> cubes = ((ModelPartAccessor) (Object) part).lodged$getCubes();
        for (ModelPart.Cube cube : cubes) {
            float minX = (cube.minX - inflationPixels) / 16.0F;
            float minY = (cube.minY - inflationPixels) / 16.0F;
            float minZ = (cube.minZ - inflationPixels) / 16.0F;
            float maxX = (cube.maxX + inflationPixels) / 16.0F;
            float maxY = (cube.maxY + inflationPixels) / 16.0F;
            float maxZ = (cube.maxZ + inflationPixels) / 16.0F;

            edge(poseStack, minX, minY, minZ, maxX, minY, minZ);
            edge(poseStack, maxX, minY, minZ, maxX, maxY, minZ);
            edge(poseStack, maxX, maxY, minZ, minX, maxY, minZ);
            edge(poseStack, minX, maxY, minZ, minX, minY, minZ);
            edge(poseStack, minX, minY, maxZ, maxX, minY, maxZ);
            edge(poseStack, maxX, minY, maxZ, maxX, maxY, maxZ);
            edge(poseStack, maxX, maxY, maxZ, minX, maxY, maxZ);
            edge(poseStack, minX, maxY, maxZ, minX, minY, maxZ);
            edge(poseStack, minX, minY, minZ, minX, minY, maxZ);
            edge(poseStack, maxX, minY, minZ, maxX, minY, maxZ);
            edge(poseStack, maxX, maxY, minZ, maxX, maxY, maxZ);
            edge(poseStack, minX, maxY, minZ, minX, maxY, maxZ);
        }
    }

    private static void edge(
            PoseStack poseStack,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ) {
        float normalX = toX - fromX;
        float normalY = toY - fromY;
        float normalZ = toZ - fromZ;
        PoseStack.Pose pose = poseStack.last();
        pose.transformNormal(normalX, normalY, normalZ, TRANSFORMED_NORMAL);
        pose.pose().transformPosition(fromX, fromY, fromZ, TRANSFORMED_POSITION);
        vertex(TRANSFORMED_POSITION, TRANSFORMED_NORMAL);
        pose.pose().transformPosition(toX, toY, toZ, TRANSFORMED_POSITION);
        vertex(TRANSFORMED_POSITION, TRANSFORMED_NORMAL);
    }

    private static void vertex(Vector3f position, Vector3f normal) {
        ensureCapacity(vertexDataSize + FLOATS_PER_VERTEX);
        vertexData[vertexDataSize++] = position.x();
        vertexData[vertexDataSize++] = position.y();
        vertexData[vertexDataSize++] = position.z();
        vertexData[vertexDataSize++] = normal.x();
        vertexData[vertexDataSize++] = normal.y();
        vertexData[vertexDataSize++] = normal.z();
    }

    private static void ensureCapacity(int required) {
        if (required <= vertexData.length) {
            return;
        }
        int capacity = Math.max(required, vertexData.length * 2);
        vertexData = Arrays.copyOf(vertexData, capacity);
    }
}
