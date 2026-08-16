package com.jaguarm.neoprogressiveautomation.world.crumble;

import java.util.function.Consumer;

import com.jaguarm.neoprogressiveautomation.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * Makes ore crumble instead of breaking, for any ore block including modded ones.
 *
 * <p>This is the compromise between Minecraft's one-block-one-break world and Factorio's
 * abstract resource patches: the ore stays physically present and visibly wears down over
 * several harvests instead of vanishing on the first hit. Because it keys off the ores tag
 * and the block's own loot table rather than a bespoke block, it works with whatever ores
 * are installed, and vein mods can supply the big patches.
 *
 * <p>Progress lives in {@link OreCrumbleState} rather than on the block, since a property
 * cannot be added to another mod's block. The visible wear reuses vanilla's block-breaking
 * crack overlay, which already renders over arbitrary blocks and syncs to clients for free.
 */
public final class OreCrumbling {

    /**
     * Base for the synthetic breaker ids passed to destroyBlockProgress. Those ids are
     * normally entity ids; offsetting far past any plausible entity id keeps a crumbling
     * ore from cancelling out a player's own break animation.
     */
    private static final int BREAKER_ID_BASE = 1_000_000;

    private OreCrumbling() {}

    /** True if this block should crumble rather than break outright. */
    public static boolean crumbles(BlockState state) {
        return Config.CRUMBLING_ORES.get() && state.is(Tags.Blocks.ORES);
    }

    /**
     * Takes one harvest out of an ore.
     *
     * <p>Drops the block's normal loot each time, so a vein yields its full contents over
     * its lifetime rather than being diluted; what changes is that extracting it takes
     * several passes instead of one.
     *
     * @return true if this harvest exhausted the ore, meaning the caller should now treat
     *         the block as broken
     */
    public static boolean harvest(ServerLevel level, BlockPos pos, BlockState state,
            ItemStack tool, @Nullable LivingEntity breaker) {
        return harvest(level, pos, state, tool, breaker, drop -> Block.popResource(level, pos, drop));
    }

    /**
     * As {@link #harvest}, but hands each drop to {@code collector} instead of throwing it
     * on the floor, so a machine can put the yield in its own output slots.
     */
    public static boolean harvest(ServerLevel level, BlockPos pos, BlockState state,
            ItemStack tool, @Nullable LivingEntity breaker, Consumer<ItemStack> collector) {
        OreCrumbleState crumble = level.getDataStorage().computeIfAbsent(OreCrumbleState.TYPE);
        int total = Config.CRUMBLE_HARVESTS.get();
        int remaining = crumble.remainingAt(pos, total);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block.getDrops(state, level, pos, blockEntity, breaker, tool).forEach(collector);

        int left = remaining - 1;
        if (left <= 0) {
            crumble.clear(pos);
            clearVisualProgress(level, pos);
            return true;
        }

        crumble.set(pos, left);
        showVisualProgress(level, pos, left, total);
        return false;
    }

    /** Paints the vanilla crack overlay in proportion to how worn the ore is. */
    private static void showVisualProgress(ServerLevel level, BlockPos pos, int left, int total) {
        int stage = (int) Math.round((1.0 - (double) left / total) * 9.0);
        level.destroyBlockProgress(breakerId(pos), pos, Math.clamp(stage, 0, 9));
    }

    private static void clearVisualProgress(ServerLevel level, BlockPos pos) {
        // -1 is vanilla's "no progress" sentinel; without this the cracks would linger on
        // whatever block ends up at this position next.
        level.destroyBlockProgress(breakerId(pos), pos, -1);
    }

    /** A stable per-position id, so two ores being worked at once do not fight. */
    private static int breakerId(BlockPos pos) {
        return BREAKER_ID_BASE + (pos.hashCode() & 0x00FF_FFFF);
    }
}
