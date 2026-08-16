package com.jaguarm.neoprogressiveautomation.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NeoProgressiveAutomation.MODID);

    /** The two drills, keyed by tier and in progression order. */
    public static final Map<MachineTier, DeferredBlock<MinerBlock>> DRILLS = new LinkedHashMap<>();

    static {
        register(MachineTier.BURNER, MapColor.STONE, SoundType.STONE, 3.5f);
        register(MachineTier.ELECTRIC, MapColor.METAL, SoundType.METAL, 4.5f);
    }

    private static void register(MachineTier tier, MapColor colour, SoundType sound, float strength) {
        DRILLS.put(tier, BLOCKS.registerBlock(
                tier.id(),
                properties -> new MinerBlock(properties, tier),
                properties -> properties
                        .mapColor(colour)
                        .strength(strength)
                        .sound(sound)));
    }

    private ModBlocks() {}
}
