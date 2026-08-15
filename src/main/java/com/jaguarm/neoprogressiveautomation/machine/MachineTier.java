package com.jaguarm.neoprogressiveautomation.machine;

/**
 * The four progression tiers shared by every machine in the mod.
 *
 * <p>A machine's tier caps which range upgrades it accepts: a stone miner takes stone
 * upgrades but rejects diamond ones. {@code maxRangeUpgrades} is how many range upgrades
 * fit in the upgrade slot at this tier.
 */
public enum MachineTier {
    WOOD("wood", 4),
    STONE("stone", 8),
    IRON("iron", 16),
    DIAMOND("diamond", 32);

    private final String id;
    private final int maxRangeUpgrades;

    MachineTier(String id, int maxRangeUpgrades) {
        this.id = id;
        this.maxRangeUpgrades = maxRangeUpgrades;
    }

    public String id() {
        return id;
    }

    public int maxRangeUpgrades() {
        return maxRangeUpgrades;
    }

    /** True if this machine can accept an upgrade built for {@code upgradeTier}. */
    public boolean accepts(MachineTier upgradeTier) {
        return upgradeTier.ordinal() <= this.ordinal();
    }
}
