package com.jvn.lodged.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jvn.lodged.Lodged;
import com.jvn.toucanlib.util.ToucanColorValues;
import com.jvn.toucanlib.util.ToucanDatapackJson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class BloodColors extends SimpleJsonResourceReloadListener {
    public static final int DEFAULT_COLOR = 0xC81E2A;
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "lodged/blood_colors";
    private static final BloodColors INSTANCE = new BloodColors();

    private static Map<EntityType<?>, Integer> entityColors = Map.of();
    private static List<TagColor> tagColors = List.of();

    private BloodColors() {
        super(GSON, DIRECTORY);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static int colorFor(LivingEntity entity) {
        Integer entityColor = entityColors.get(entity.getType());
        if (entityColor != null) {
            return entityColor;
        }

        for (TagColor tagColor : tagColors) {
            if (entity.getType().is(tagColor.tag())) {
                return tagColor.color();
            }
        }

        return DEFAULT_COLOR;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<EntityType<?>, Integer> loadedEntityColors = new HashMap<>();
        List<TagColor> loadedTagColors = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> load(entry.getKey(), entry.getValue(), loadedEntityColors, loadedTagColors));
        entityColors = Map.copyOf(loadedEntityColors);
        tagColors = List.copyOf(loadedTagColors);
    }

    private static void load(
            ResourceLocation id,
            JsonElement element,
            Map<EntityType<?>, Integer> loadedEntityColors,
            List<TagColor> loadedTagColors) {
        try {
            JsonObject json = GsonHelper.convertToJsonObject(element, "blood color");
            int color = ToucanColorValues.requireRgb(GsonHelper.getNonNull(json, "color"));
            for (JsonElement entityTypeElement : GsonHelper.getAsJsonArray(json, "entity_types")) {
                String entry = GsonHelper.convertToString(entityTypeElement, "entity type");
                if (entry.startsWith("#")) {
                    ResourceLocation tagId = parseId(entry.substring(1), id);
                    loadedTagColors.add(new TagColor(TagKey.create(Registries.ENTITY_TYPE, tagId), color));
                    continue;
                }

                ResourceLocation entityId = parseId(entry, id);
                BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                        .ifPresentOrElse(
                                entityType -> loadedEntityColors.put(entityType, color),
                                () -> Lodged.LOGGER.warn(
                                        "Ignoring unknown entity type '{}' in blood color file {}",
                                        entityId,
                                        id));
            }
        } catch (IllegalArgumentException | JsonParseException exception) {
            Lodged.LOGGER.warn("Failed to load blood color file {}", id, exception);
        }
    }

    private static ResourceLocation parseId(String id, ResourceLocation fileId) {
        return ToucanDatapackJson.parseResourceLocation(id)
                .orElseThrow(() -> new JsonParseException(
                        "Invalid resource location '" + id + "' in " + fileId));
    }

    private record TagColor(TagKey<EntityType<?>> tag, int color) {
    }
}
