package com.jaguarm.neoprogressiveautomation;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for Neo Progressive Automation.
 *
 * <p>Option names and defaults follow the original 1.12.2 mod where the mechanic still
 * makes sense on modern Minecraft. {@link #MINE_FLOOR} is new: 1.12.2 worlds bottomed out
 * at y=0, so the original hardcoded that, but modern worlds reach y=-64.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -- Range ------------------------------------------------------------------

    public static final ModConfigSpec.IntValue INITIAL_RANGE = BUILDER
            .comment("Radius in blocks a machine covers with no upgrades installed.")
            .defineInRange("range.initialRange", 1, 1, 128);

    public static final ModConfigSpec.IntValue UPGRADE_RANGE = BUILDER
            .comment("Additional radius granted per range upgrade.")
            .defineInRange("range.upgradeRange", 1, 1, 128);

    // -- Miner ------------------------------------------------------------------

    public static final ModConfigSpec.IntValue MINE_FLOOR = BUILDER
            .comment(
                    "Lowest Y level the miner will dig to.",
                    "Clamped to the dimension's own floor, so -64 means 'all the way down'",
                    "in the overworld but stops at bedrock in the nether.")
            .defineInRange("miner.mineFloor", -64, -2048, 2048);

    public static final ModConfigSpec.BooleanValue REQUIRE_COBBLE_BACKFILL = BUILDER
            .comment(
                    "Whether the miner backfills each mined block with cobblestone,",
                    "consuming one cobblestone per block. Turning this off makes the miner",
                    "leave air behind and removes the cobble requirement entirely, which",
                    "also makes the cobble generator upgrade pointless.")
            .define("miner.requireCobbleBackfill", true);

    public static final ModConfigSpec.BooleanValue DESTROY_TOOLS = BUILDER
            .comment("Whether tools are consumed when their durability runs out.")
            .define("miner.destroyTools", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
