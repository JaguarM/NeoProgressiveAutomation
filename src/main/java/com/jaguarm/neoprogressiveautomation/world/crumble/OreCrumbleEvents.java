package com.jaguarm.neoprogressiveautomation.world.crumble;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Applies crumbling to ore broken by hand, so a player and a drill see the same world.
 *
 * <p>Without this the mechanic would only exist for machines, and mining an ore yourself
 * would still make it vanish in one hit — which would read as a bug rather than a design.
 */
@EventBusSubscriber(modid = NeoProgressiveAutomation.MODID)
public final class OreCrumbleEvents {

    private OreCrumbleEvents() {}

    @SubscribeEvent
    static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!OreCrumbling.crumbles(event.getState())) {
            return;
        }

        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        // Creative mode keeps vanilla behaviour: someone clearing terrain in creative does
        // not want to hit the same block eight times.
        if (player != null && player.isCreative()) {
            return;
        }

        ItemStack tool = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        boolean exhausted = OreCrumbling.harvest(level, event.getPos(), event.getState(), tool, player);

        if (!exhausted) {
            // Leave the ore standing; it has more in it. Cancelling suppresses the vanilla
            // break and its drops, which OreCrumbling has already handled.
            event.setCanceled(true);
        }
    }
}
