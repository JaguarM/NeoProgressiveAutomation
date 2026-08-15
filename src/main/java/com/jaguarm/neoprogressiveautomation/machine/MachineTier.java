package com.jaguarm.neoprogressiveautomation.machine;

/**
 * The four progression tiers shared by every machine in the mod.
 *
 * <p>A machine's tier caps which range upgrades it accepts: a stone miner takes stone
 * upgrades but rejects diamond ones. {@code maxRangeUpgrades} is how many range upgrades
 * fit in the upgrade slot at this tier.
 */
public enum MachineTier implements net.minecraft.util.StringRepresentable {
    WOOD("wood", 4),
    STONE("stone", 8),
    IRON("iron", 16),
    DIAMOND("diamond", 32);

    public static final com.mojang.serialization.Codec<MachineTier> CODEC =
            net.minecraft.util.StringRepresentable.fromEnum(MachineTier::values);

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

    @Override
    public String getSerializedName() {
        return id;
    }

    /**
     * True if this machine accepts a range upgrade built for {@code upgradeTier}.
     *
     * <p>Exact match, not "this tier or lower". A stone miner rejects wooden upgrades just
     * as it rejects diamond ones, matching the original mod's per-tier upgrade types.
     */
    public boolean accepts(MachineTier upgradeTier) {
        return upgradeTier == this;
    }
}
