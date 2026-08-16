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

    public static final ModConfigSpec.DoubleValue MINING_SPEED_MULTIPLIER = BUILDER
            .comment(
                    "How fast machines mine, relative to a player swinging the same tool.",
                    "1.0 matches vanilla exactly: a machine with an iron pickaxe takes as",
                    "long on stone as you would. Above 1.0 is faster, below is slower.",
                    "Applied on top of vanilla's formula rather than replacing it, so tool",
                    "tier, block hardness and Efficiency all still behave as expected.")
            .defineInRange("miner.speedMultiplier", 1.0, 0.05, 100.0);

    public static final ModConfigSpec.BooleanValue DESTROY_TOOLS = BUILDER
            .comment("Whether tools are consumed when their durability runs out.")
            .define("miner.destroyTools", true);

    // -- Crumbling ores ---------------------------------------------------------

    public static final ModConfigSpec.BooleanValue CRUMBLING_ORES = BUILDER
            .comment(
                    "Whether ore blocks crumble instead of breaking outright.",
                    "A crumbling ore stays in the world and wears down visibly over several",
                    "harvests, dropping its loot each time. Applies to anything in the ores",
                    "tag, so modded ores and ores placed by vein mods are included.")
            .define("crumbling.enabled", true);

    public static final ModConfigSpec.IntValue CRUMBLE_HARVESTS = BUILDER
            .comment(
                    "How many harvests it takes to exhaust one ore block.",
                    "Each harvest drops the block's normal loot, so raising this raises the",
                    "total yield of every ore in the world as well as the time to extract it.")
            .defineInRange("crumbling.harvestsPerOre", 8, 1, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
