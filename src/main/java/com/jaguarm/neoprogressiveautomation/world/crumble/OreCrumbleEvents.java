package com.jaguarm.neoprogressiveautomation.world.crumble;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Hooks that keep crumbling honest when ore is broken, pushed or blown up.
 *
 * <p>Progress is keyed by position rather than stored on the block, which is the only way
 * to support ores from other mods. The cost of that is decoupling: the physical block and
 * its progress can drift apart, so anything that moves or destroys a block without going
 * through the break event has to be accounted for.
 */
@EventBusSubscriber(modid = NeoProgressiveAutomation.MODID)
public final class OreCrumbleEvents {

    private OreCrumbleEvents() {}

    /**
     * The four cases a break can fall into:
     *
     * <ol>
     *   <li>Pristine + Silk Touch — let vanilla take the node whole. Not a duplication:
     *       one block in, one block out, and no progress is ever recorded.
     *   <li>Pristine + ordinary tool — start crumbling.
     *   <li>Fractured + Silk Touch — refused. The enchantment is stripped and the node
     *       crumbles anyway, because handing back a whole ore on top of the harvests
     *       already taken is the duplication we are closing.
     *   <li>Fractured + ordinary tool — keep crumbling.
     * </ol>
     */
    @SubscribeEvent
    static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!OreCrumbling.crumbles(event.getState())) {
            return;
        }

        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        // Creative keeps vanilla behaviour: clearing terrain should not take eight hits.
        if (player != null && player.isCreative()) {
            OreCrumbling.forget(level, event.getPos());
            return;
        }

        ItemStack tool = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        BlockPos pos = event.getPos();

        // Case 1: hand it to vanilla untouched. Vanilla knows the real silk-touch loot,
        // damages the tool and credits the statistic; emulating all that would only be a
        // worse copy of it.
        if (OreCrumbling.silkTouchTakesWhole(level, pos, tool)) {
            return;
        }

        boolean wasFractured = OreCrumbling.isFractured(level, pos);
        boolean exhausted = OreCrumbling.harvest(level, pos, event.getState(), tool, player);

        // Case 3: say why the pickaxe did not lift it, or it just looks broken.
        if (wasFractured && OreCrumbling.hasSilkTouch(level, tool)) {
            OreCrumbling.warnCrumbleLock(level, pos, player);
        }

        if (!exhausted) {
            // Ore is still standing. Cancelling suppresses vanilla's break and its drops,
            // which harvest() has already handled.
            event.setCanceled(true);
        }
    }

    /**
     * Stops pistons shifting a fractured node.
     *
     * <p>Progress is bound to a position, so pushing the block one across would leave the
     * damage behind and present a pristine ore at the new position: a reset, and an
     * orphaned entry in the map.
     */
    @SubscribeEvent
    static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // A sticky piston retracting drags the block two out from the piston back with it.
        // Checked directly rather than left to the structure resolver: when resolve()
        // returns false there is no structure to inspect, and the earlier version simply
        // bailed out and let the pull through, which moved the ore and stranded its damage.
        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.RETRACT
                && OreCrumbling.isFractured(level, event.getPos().relative(event.getDirection(), 2))) {
            event.setCanceled(true);
            return;
        }

        var structure = event.getStructureHelper();
        if (structure == null || !structure.resolve()) {
            return;
        }

        for (BlockPos pos : structure.getToPush()) {
            if (OreCrumbling.isFractured(level, pos)) {
                event.setCanceled(true);
                return;
            }
        }

        // Blocks a piston shears off never reach the break event, so drop their tracking
        // here or it lingers on a position that no longer holds ore.
        structure.getToDestroy().forEach(pos -> OreCrumbling.forget(level, pos));
    }

    /**
     * Drops tracking for ore destroyed by an explosion, which never reaches the break
     * event. Without this the map would accumulate entries for positions that no longer
     * hold ore, and a future ore placed there would inherit somebody else's damage.
     */
    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.getAffectedBlocks().forEach(pos -> OreCrumbling.forget(level, pos));
    }
}
