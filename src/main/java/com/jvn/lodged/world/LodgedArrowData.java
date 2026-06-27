package com.jvn.lodged.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public record LodgedArrowData(
        ItemStack stack,
        boolean recoverable,
        boolean causesBleeding,
        boolean fromPlayer,
        boolean infinityGenerated,
        boolean creativeGenerated,
        long gameTime,
        LodgedArrowVisual visual) {
    private static final String STACK_KEY = "stack";
    private static final String RECOVERABLE_KEY = "recoverable";
    private static final String CAUSES_BLEEDING_KEY = "causes_bleeding";
    private static final String FROM_PLAYER_KEY = "from_player";
    private static final String INFINITY_GENERATED_KEY = "infinity_generated";
    private static final String CREATIVE_GENERATED_KEY = "creative_generated";
    private static final String GAME_TIME_KEY = "game_time";

    public LodgedArrowData {
        stack = stack.copyWithCount(1);
        visual = visual.withStack(stack);
    }

    CompoundTag save(Entity entity) {
        CompoundTag tag = new CompoundTag();
        tag.put(STACK_KEY, stack.copyWithCount(1).save(entity.registryAccess()));
        tag.putBoolean(RECOVERABLE_KEY, recoverable);
        tag.putBoolean(CAUSES_BLEEDING_KEY, causesBleeding);
        tag.putBoolean(FROM_PLAYER_KEY, fromPlayer);
        tag.putBoolean(INFINITY_GENERATED_KEY, infinityGenerated);
        tag.putBoolean(CREATIVE_GENERATED_KEY, creativeGenerated);
        tag.putLong(GAME_TIME_KEY, gameTime);
        visual.save(tag);
        return tag;
    }

    static LodgedArrowData load(Entity entity, CompoundTag tag) {
        ItemStack stack = ItemStack.parse(entity.registryAccess(), tag.get(STACK_KEY))
                .orElse(ItemStack.EMPTY)
                .copyWithCount(1);

        return new LodgedArrowData(
                stack,
                !tag.contains(RECOVERABLE_KEY) || tag.getBoolean(RECOVERABLE_KEY),
                !tag.contains(CAUSES_BLEEDING_KEY) || tag.getBoolean(CAUSES_BLEEDING_KEY),
                tag.getBoolean(FROM_PLAYER_KEY),
                tag.getBoolean(INFINITY_GENERATED_KEY),
                tag.getBoolean(CREATIVE_GENERATED_KEY),
                tag.getLong(GAME_TIME_KEY),
                LodgedArrowVisual.load(tag));
    }
}
