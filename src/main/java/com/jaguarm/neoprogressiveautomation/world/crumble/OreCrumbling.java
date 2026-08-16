package com.jaguarm.neoprogressiveautomation.world.crumble;

import java.util.function.Consumer;

import com.jaguarm.neoprogressiveautomation.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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

    /** True if this position has already been worked and is being tracked. */
    public static boolean isFractured(ServerLevel level, BlockPos pos) {
        return level.getDataStorage().computeIfAbsent(OreCrumbleState.TYPE).isTracked(pos);
    }

    /**
     * Whether Silk Touch should simply take a pristine node whole.
     *
     * <p>Callers let vanilla do the work in this case rather than emulating it: vanilla
     * knows the block's real silk-touch loot, which is not always "one of itself", and it
     * also handles tool damage and statistics. An untouched node moved this way is not a
     * duplication, because exactly one block goes in and one comes out.
     */
    public static boolean silkTouchTakesWhole(ServerLevel level, BlockPos pos, ItemStack tool) {
        return !isFractured(level, pos) && hasSilkTouch(level, tool);
    }

    /** Tells the player why their Silk Touch pickaxe did not lift the node. */
    public static void warnCrumbleLock(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
        level.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
        if (player != null) {
            // overlay = true puts it on the action bar, where a repeated message will not
            // spam the chat log.
            player.sendSystemMessage(
                    Component.translatable("message.neoprogressiveautomation.crumble_lock")
                            .withStyle(ChatFormatting.GRAY),
                    true);
        }
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
        // Silk Touch never yields an intact ore here. A pristine node is handled before
        // this method is ever reached; anything that gets this far is already fractured,
        // and handing back a whole ore on top of the harvests already taken out of it is
        // the duplication exploit: partially mine, silk touch, replace, repeat.
        if (hasSilkTouch(level, tool)) {
            tool = withoutSilkTouch(level, tool);
        }

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

    /**
     * Re-sends the crack overlay for every tracked position, and prunes entries whose
     * block is no longer crumbling ore.
     *
     * <p>destroyBlockProgress is fire-and-forget: the server sends it once, only to players
     * within 32 blocks, and the client discards entries it has not heard about for 400
     * ticks. Nothing replays it on world load or when a player walks up. Since crumble
     * progress is durable but its visual is not, the visual has to be refreshed.
     *
     * <p>The prune covers every way a block can be replaced without an event we hook —
     * /setblock, worldedit, another mod swapping the block — turning "inherits a stranger's
     * damage" into "starts fresh" without having to enumerate the routes.
     */
    public static void refreshVisuals(ServerLevel level) {
        OreCrumbleState crumble = level.getDataStorage().computeIfAbsent(OreCrumbleState.TYPE);
        int total = Config.CRUMBLE_HARVESTS.get();

        for (BlockPos pos : crumble.trackedPositions()) {
            // Leave unloaded chunks alone: nobody can see them, and touching them would
            // force chunk loads for no benefit.
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!crumbles(level.getBlockState(pos))) {
                crumble.clear(pos);
                clearVisualProgress(level, pos);
                continue;
            }
            showVisualProgress(level, pos, crumble.remainingAt(pos, total), total);
        }
    }

    /** Drops any tracking for a position, and clears its crack overlay. */
    public static void forget(ServerLevel level, BlockPos pos) {
        OreCrumbleState crumble = level.getDataStorage().computeIfAbsent(OreCrumbleState.TYPE);
        if (crumble.isTracked(pos)) {
            crumble.clear(pos);
            clearVisualProgress(level, pos);
        }
    }

    public static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        return !tool.isEmpty() && silkTouchLevel(level, tool) > 0;
    }

    private static int silkTouchLevel(ServerLevel level, ItemStack tool) {
        Holder<Enchantment> silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool);
    }

    /**
     * A copy of the tool with Silk Touch removed, so the ore yields its ordinary loot.
     * Copied rather than modified in place, and only Silk Touch is stripped, so Fortune on
     * the same tool still applies.
     */
    private static ItemStack withoutSilkTouch(ServerLevel level, ItemStack tool) {
        Holder<Enchantment> silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        ItemStack copy = tool.copy();
        EnchantmentHelper.updateEnchantments(copy, enchantments -> enchantments.removeIf(silkTouch::equals));
        return copy;
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
