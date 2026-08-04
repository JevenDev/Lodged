package com.jvn.lodged.modelgen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.SharedConstants;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VanillaModelHitboxGenerator {
    private static final Field CHILDREN = field("children");
    private static final Field CUBES = field("cubes");

    private VanillaModelHitboxGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        List<String> lines = new ArrayList<>();
        LayerDefinitions.createRoots().entrySet().stream()
                .filter(entry -> entry.getKey().getLayer().equals("main"))
                .filter(entry -> entry.getKey().getModel().getNamespace().equals("minecraft"))
                .sorted(Comparator.comparing(entry -> entry.getKey().getModel().getPath()))
                .forEach(entry -> collectModel(entry.getKey(), entry.getValue().bakeRoot(), lines));
        Files.createDirectories(output.getParent());
        Files.write(output, lines);
        System.out.printf(Locale.ROOT, "Generated %,d vanilla model cuboids for %,d layers%n",
                lines.size(), lines.stream().map(line -> line.substring(0, line.indexOf('\t'))).distinct().count());
    }

    private static void collectModel(ModelLayerLocation layer, ModelPart root, List<String> output) {
        PoseStack poseStack = new PoseStack();
        collect(layer.getModel().getPath(), root, "", poseStack, output);
    }

    @SuppressWarnings("unchecked")
    private static void collect(
            String model,
            ModelPart part,
            String path,
            PoseStack poseStack,
            List<String> output) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Matrix4f transform = poseStack.last().pose();
        Vector3f origin = new Vector3f();
        transform.transformPosition(origin);
        List<ModelPart.Cube> cubes;
        Map<String, ModelPart> children;
        try {
            cubes = (List<ModelPart.Cube>) CUBES.get(part);
            children = (Map<String, ModelPart>) CHILDREN.get(part);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }

        for (int index = 0; index < cubes.size(); index++) {
            ModelPart.Cube cube = cubes.get(index);
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (int mask = 0; mask < 8; mask++) {
                Vector3f point = new Vector3f(
                        ((mask & 1) == 0 ? cube.minX : cube.maxX) / 16.0F,
                        ((mask & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
                        ((mask & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F);
                transform.transformPosition(point);
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                minZ = Math.min(minZ, point.z);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
                maxZ = Math.max(maxZ, point.z);
            }
            String partPath = path.isEmpty() ? "root" : path;
            output.add(String.join("\t",
                    model,
                    model + "/" + partPath + "/" + index,
                    partPath,
                    bodyPart(partPath),
                    Float.toString(origin.x),
                    Float.toString(origin.y),
                    Float.toString(origin.z),
                    Float.toString(minX - origin.x),
                    Float.toString(minY - origin.y),
                    Float.toString(minZ - origin.z),
                    Float.toString(maxX - origin.x),
                    Float.toString(maxY - origin.y),
                    Float.toString(maxZ - origin.z)));
        }

        children.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                collect(model, entry.getValue(), path.isEmpty() ? entry.getKey() : path + "/" + entry.getKey(), poseStack, output));
        poseStack.popPose();
    }

    private static String bodyPart(String path) {
        String name = path.toLowerCase(Locale.ROOT);
        if (name.contains("head") || name.contains("neck") || name.contains("nose")
                || name.contains("jaw") || name.contains("beak")) {
            return "head";
        }
        boolean left = name.contains("left") || name.endsWith("_l") || name.contains("/l_");
        boolean right = name.contains("right") || name.endsWith("_r") || name.contains("/r_");
        boolean arm = name.contains("arm") || name.contains("wing") || name.contains("hand");
        boolean leg = name.contains("leg") || name.contains("foot") || name.contains("knee");
        if (left && arm) return "left_arm";
        if (right && arm) return "right_arm";
        if (left && leg) return "left_leg";
        if (right && leg) return "right_leg";
        if (arm) return "right_arm";
        if (leg) return "right_leg";
        return "chest";
    }

    private static Field field(String name) {
        try {
            Field field = ModelPart.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
