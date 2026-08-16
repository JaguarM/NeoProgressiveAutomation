package com.jaguarm.neoprogressiveautomation.machine;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The four progression tiers shared by every machine in the mod.
 *
 * <p>Tiers no longer gate which upgrades fit — modules are universal. What a tier buys is
 * {@linkplain #moduleSlots() module slots}, so progression is about how many trade-offs
 * you can run at once. The wooden miner has none, matching Factorio's burner drill: it
 * works, it just cannot be tuned, which is what makes tiering up worth doing.
 */
public enum MachineTier implements StringRepresentable {
    WOOD("wood", 0),
    STONE("stone", 1),
    IRON("iron", 2),
    DIAMOND("diamond", 3);

    /** Every miner reserves this many module slots; tiers differ in how many are usable. */
    public static final int MAX_MODULE_SLOTS = 3;

    public static final Codec<MachineTier> CODEC = StringRepresentable.fromEnum(MachineTier::values);

    private final String id;
    private final int moduleSlots;

    MachineTier(String id, int moduleSlots) {
        this.id = id;
        this.moduleSlots = moduleSlots;
    }

    public String id() {
        return id;
    }

    /** How many of the reserved module slots this tier can actually use. */
    public int moduleSlots() {
        return moduleSlots;
    }

    /** True if the module slot at {@code index} is unlocked at this tier. */
    public boolean hasModuleSlot(int index) {
        return index >= 0 && index < moduleSlots;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
