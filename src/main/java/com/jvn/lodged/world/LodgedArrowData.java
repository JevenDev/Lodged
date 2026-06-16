package com.jvn.lodged.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

record LodgedArrowData(ItemStack stack, boolean fromPlayer, boolean infinityGenerated, boolean creativeGenerated, long gameTime) {
    private static final String STACK_KEY = "stack";
    private static final String FROM_PLAYER_KEY = "from_player";
    private static final String INFINITY_GENERATED_KEY = "infinity_generated";
    private static final String CREATIVE_GENERATED_KEY = "creative_generated";
    private static final String GAME_TIME_KEY = "game_time";

    CompoundTag save(Entity entity) {
        CompoundTag tag = new CompoundTag();
        tag.put(STACK_KEY, stack.copyWithCount(1).save(entity.registryAccess()));
        tag.putBoolean(FROM_PLAYER_KEY, fromPlayer);
        tag.putBoolean(INFINITY_GENERATED_KEY, infinityGenerated);
        tag.putBoolean(CREATIVE_GENERATED_KEY, creativeGenerated);
        tag.putLong(GAME_TIME_KEY, gameTime);
        return tag;
    }

    static LodgedArrowData load(Entity entity, CompoundTag tag) {
        ItemStack stack = ItemStack.parse(entity.registryAccess(), tag.get(STACK_KEY))
                .orElse(ItemStack.EMPTY)
                .copyWithCount(1);

        return new LodgedArrowData(
                stack,
                tag.getBoolean(FROM_PLAYER_KEY),
                tag.getBoolean(INFINITY_GENERATED_KEY),
                tag.getBoolean(CREATIVE_GENERATED_KEY),
                tag.getLong(GAME_TIME_KEY));
    }
}
