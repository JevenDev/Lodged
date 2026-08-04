package com.jvn.lodged.collision;

import com.jvn.lodged.Lodged;
import com.jvn.lodged.world.LodgedArrowBodyPart;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.TropicalFish;

final class VanillaModelHitboxRegistry {
    private static final Map<String, ModelColliderPart[]> MODELS = load();

    private VanillaModelHitboxRegistry() {
    }

    static ModelColliderPart[] parts(LivingEntity entity) {
        return MODELS.get(modelKey(entity));
    }

    static boolean supports(LivingEntity entity) {
        return parts(entity) != null;
    }

    static double renderScale(LivingEntity entity) {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return switch (key) {
            case "giant" -> 6.0D;
            case "donkey" -> 0.87D;
            case "mule" -> 0.92D;
            case "elder_guardian" -> 2.35D;
            case "wither_skeleton" -> 1.2D;
            default -> 1.0D;
        };
    }

    private static String modelKey(LivingEntity entity) {
        if (entity instanceof Pufferfish pufferfish) {
            return switch (pufferfish.getPuffState()) {
                case 0 -> "pufferfish_small";
                case 1 -> "pufferfish_medium";
                default -> "pufferfish_big";
            };
        }
        if (entity instanceof TropicalFish fish) {
            return fish.getVariant().base() == TropicalFish.Base.LARGE
                    ? "tropical_fish_large"
                    : "tropical_fish_small";
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key.getNamespace().equals("minecraft") ? key.getPath() : "";
    }

    private static Map<String, ModelColliderPart[]> load() {
        Map<String, List<ModelColliderPart>> mutable = new HashMap<>();
        InputStream stream = VanillaModelHitboxRegistry.class.getClassLoader()
                .getResourceAsStream("assets/lodged/model_hitboxes.tsv");
        if (stream == null) {
            Lodged.LOGGER.error("Missing generated vanilla model hitbox data; unsupported entities will use vanilla collision");
            return Map.of();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 13) {
                    continue;
                }
                mutable.computeIfAbsent(fields[0], ignored -> new ArrayList<>()).add(
                        new GeneratedModelHitboxPart(
                                fields[1],
                                fields[2],
                                LodgedArrowBodyPart.bySerializedName(fields[3]),
                                Double.parseDouble(fields[4]),
                                Double.parseDouble(fields[5]),
                                Double.parseDouble(fields[6]),
                                Double.parseDouble(fields[7]),
                                Double.parseDouble(fields[8]),
                                Double.parseDouble(fields[9]),
                                Double.parseDouble(fields[10]),
                                Double.parseDouble(fields[11]),
                                Double.parseDouble(fields[12])));
            }
        } catch (IOException | RuntimeException exception) {
            Lodged.LOGGER.error("Could not load generated vanilla model hitbox data", exception);
            return Map.of();
        }

        Map<String, ModelColliderPart[]> result = new HashMap<>();
        mutable.forEach((key, value) -> result.put(key, value.toArray(ModelColliderPart[]::new)));
        return Map.copyOf(result);
    }
}
