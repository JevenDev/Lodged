package com.jvn.lodged.client;

import com.jvn.lodged.world.LodgedArrowVisual;
import com.jvn.lodged.mixin.client.ModelPartAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LodgedArrowRenderHelper {
    private LodgedArrowRenderHelper() {
    }

    public static AbstractArrow createRenderedArrow(Level level, double x, double y, double z, LodgedArrowVisual arrow) {
        return createRenderedArrow(
                level,
                x,
                y,
                z,
                arrow,
                arrow.directionX(),
                arrow.directionY(),
                arrow.directionZ());
    }

    public static AbstractArrow createRenderedArrow(
            Level level,
            double x,
            double y,
            double z,
            LodgedArrowVisual arrow,
            Vector3f direction) {
        return createRenderedArrow(level, x, y, z, arrow, direction.x(), direction.y(), direction.z());
    }

    private static AbstractArrow createRenderedArrow(
            Level level,
            double x,
            double y,
            double z,
            LodgedArrowVisual arrow,
            float directionX,
            float directionY,
            float directionZ) {
        ItemStack stack = renderStack(arrow);
        AbstractArrow renderedArrow = stack.is(Items.SPECTRAL_ARROW)
                ? new SpectralArrow(level, x, y, z, stack, null)
                : new Arrow(level, x, y, z, stack, null);

        float horizontalLength = Mth.sqrt(directionX * directionX + directionZ * directionZ);
        renderedArrow.setYRot((float) (Math.atan2(directionX, directionZ) * 180.0F / Math.PI));
        renderedArrow.setXRot((float) (Math.atan2(directionY, horizontalLength) * 180.0F / Math.PI));
        renderedArrow.yRotO = renderedArrow.getYRot();
        renderedArrow.xRotO = renderedArrow.getXRot();
        return renderedArrow;
    }

    public static Vector3f translateToModelPart(
            PoseStack poseStack,
            EntityModel<?> model,
            LodgedArrowVisual arrow) {
        if (model instanceof HumanoidModel<?> humanoidModel) {
            return translateToHumanoidPart(poseStack, humanoidModel, arrow);
        }
        if (arrow.modelPartPath() == null) {
            return null;
        }

        List<ModelPart> chain = resolvePartChain(model, arrow.modelPartPath());
        if (chain == null || chain.isEmpty()) {
            return null;
        }
        PartAnchor anchor = partAnchor(chain, arrow);
        for (ModelPart part : chain) {
            part.translateAndRotate(poseStack);
        }
        poseStack.translate(
                anchor.localPosition().x() / 16.0F,
                anchor.localPosition().y() / 16.0F,
                anchor.localPosition().z() / 16.0F);
        return anchor.localDirection();
    }


    private static List<ModelPart> resolvePartChain(EntityModel<?> model, String path) {
        String[] names = path.split("/");
        try {
            if (model instanceof HierarchicalModel<?> hierarchicalModel) {
                List<ModelPart> chain = new ArrayList<>();
                ModelPart current = hierarchicalModel.root();
                chain.add(current);
                for (String name : names) {
                    if (name.equals("root") || name.isEmpty()) {
                        continue;
                    }
                    current = current.getChild(name);
                    chain.add(current);
                }
                return chain;
            }

            String topName = names[0];
            ModelPart current = topLevelPart(model, topName);
            if (current == null) {
                return null;
            }
            List<ModelPart> chain = new ArrayList<>();
            chain.add(current);
            for (int index = 1; index < names.length; index++) {
                current = current.getChild(names[index]);
                chain.add(current);
            }
            return chain;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static ModelPart topLevelPart(EntityModel<?> model, String name) throws ReflectiveOperationException {
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == ModelPart.class && field.getName().equals(name)) {
                    field.setAccessible(true);
                    return (ModelPart) field.get(model);
                }
            }
        }
        return null;
    }

    private static PartAnchor partAnchor(List<ModelPart> chain, LodgedArrowVisual arrow) {
        Matrix4f restTransform = new Matrix4f();
        for (ModelPart part : chain) {
            PartPose pose = part.getInitialPose();
            restTransform.translate(pose.x, pose.y, pose.z)
                    .rotate(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
        }
        Matrix4f inverse = restTransform.invert(new Matrix4f());
        Vector3f localPosition = inverse.transformPosition(new Vector3f(
                arrow.modelX() * 16.0F,
                arrow.modelY() * 16.0F,
                arrow.modelZ() * 16.0F));
        Vector3f localDirection = inverse.transformDirection(new Vector3f(
                arrow.directionX(),
                arrow.directionY(),
                arrow.directionZ())).normalize();
        ModelPart part = chain.getLast();
        snapToClosestCube(part, localPosition);
        return new PartAnchor(part, localPosition, localDirection);
    }
    public static Vector3f translateToHumanoidPart(
            PoseStack poseStack,
            HumanoidModel<?> model,
            LodgedArrowVisual arrow) {
        PartAnchor anchor = partAnchor(model, arrow);
        anchor.part().translateAndRotate(poseStack);
        poseStack.translate(
                anchor.localPosition().x() / 16.0F,
                anchor.localPosition().y() / 16.0F,
                anchor.localPosition().z() / 16.0F);
        return anchor.localDirection();
    }

    public static Vector3f humanoidModelPosition(HumanoidModel<?> model, LodgedArrowVisual arrow) {
        PartAnchor anchor = partAnchor(model, arrow);
        ModelPart part = anchor.part();
        Vector3f position = anchor.localPosition().mul(part.xScale, part.yScale, part.zScale);
        new Quaternionf()
                .rotationZYX(part.zRot, part.yRot, part.xRot)
                .transform(position);
        return position.add(part.x, part.y, part.z).div(16.0F);
    }

    private static PartAnchor partAnchor(HumanoidModel<?> model, LodgedArrowVisual arrow) {
        ModelPart part = switch (arrow.bodyPart()) {
            case HEAD -> model.head;
            case CHEST -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
        };
        return partAnchor(part, arrow);
    }

    private static PartAnchor partAnchor(ModelPart part, LodgedArrowVisual arrow) {
        PartPose initialPose = part.getInitialPose();
        Quaternionf inverseInitialRotation = new Quaternionf()
                .rotationZYX(initialPose.zRot, initialPose.yRot, initialPose.xRot)
                .conjugate();
        Vector3f localPosition = inverseInitialRotation.transform(new Vector3f(
                arrow.modelX() * 16.0F - initialPose.x,
                arrow.modelY() * 16.0F - initialPose.y,
                arrow.modelZ() * 16.0F - initialPose.z));
        Vector3f localDirection = inverseInitialRotation.transform(new Vector3f(
                arrow.directionX(),
                arrow.directionY(),
                arrow.directionZ()));
        snapToClosestCube(part, localPosition);
        return new PartAnchor(part, localPosition, localDirection);
    }

    private static void snapToClosestCube(ModelPart part, Vector3f point) {
        List<ModelPart.Cube> cubes = ((ModelPartAccessor) (Object) part).lodged$getCubes();
        if (cubes.isEmpty()) {
            return;
        }

        ModelPart.Cube closest = cubes.getFirst();
        float closestDistance = distanceToCubeSqr(closest, point);
        for (int index = 1; index < cubes.size(); index++) {
            ModelPart.Cube cube = cubes.get(index);
            float distance = distanceToCubeSqr(cube, point);
            if (distance < closestDistance) {
                closest = cube;
                closestDistance = distance;
            }
        }
        snapToSurface(closest, point);
    }

    private static float distanceToCubeSqr(ModelPart.Cube cube, Vector3f point) {
        float x = Mth.clamp(point.x(), cube.minX, cube.maxX);
        float y = Mth.clamp(point.y(), cube.minY, cube.maxY);
        float z = Mth.clamp(point.z(), cube.minZ, cube.maxZ);
        float dx = point.x() - x;
        float dy = point.y() - y;
        float dz = point.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void snapToSurface(ModelPart.Cube cube, Vector3f point) {
        float x = Mth.clamp(point.x(), cube.minX, cube.maxX);
        float y = Mth.clamp(point.y(), cube.minY, cube.maxY);
        float z = Mth.clamp(point.z(), cube.minZ, cube.maxZ);
        if (point.x() < cube.minX || point.x() > cube.maxX
                || point.y() < cube.minY || point.y() > cube.maxY
                || point.z() < cube.minZ || point.z() > cube.maxZ) {
            point.set(x, y, z);
            return;
        }

        float minDistance = point.x() - cube.minX;
        x = cube.minX;
        float distance = cube.maxX - point.x();
        if (distance < minDistance) {
            minDistance = distance;
            x = cube.maxX;
        }
        distance = point.y() - cube.minY;
        if (distance < minDistance) {
            minDistance = distance;
            x = point.x();
            y = cube.minY;
        }
        distance = cube.maxY - point.y();
        if (distance < minDistance) {
            minDistance = distance;
            x = point.x();
            y = cube.maxY;
        }
        distance = point.z() - cube.minZ;
        if (distance < minDistance) {
            minDistance = distance;
            x = point.x();
            y = point.y();
            z = cube.minZ;
        }
        if (cube.maxZ - point.z() < minDistance) {
            x = point.x();
            y = point.y();
            z = cube.maxZ;
        }
        point.set(x, y, z);
    }

    private static ItemStack renderStack(LodgedArrowVisual arrow) {
        ItemStack stack = arrow.stack();
        if (stack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        return stack.copyWithCount(1);
    }

    private record PartAnchor(ModelPart part, Vector3f localPosition, Vector3f localDirection) {
    }
}
