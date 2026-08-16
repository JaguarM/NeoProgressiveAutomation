package com.jaguarm.neoprogressiveautomation.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the miner's energy buffer to cables.
 *
 * <p>Only the insert-only view is published, and only on electric tiers: a burner miner
 * returns null and so looks like a plain block to an energy network, which is what stops
 * cables trying to feed a machine that cannot use power.
 */
public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.MINER.get(),
                (miner, side) -> miner.cableView());
    }
}
