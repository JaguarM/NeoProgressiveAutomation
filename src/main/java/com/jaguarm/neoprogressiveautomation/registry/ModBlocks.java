package com.jaguarm.neoprogressiveautomation.registry;

import java.util.List;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NeoProgressiveAutomation.MODID);

    public static final DeferredBlock<MinerBlock> MINER_WOOD =
            miner(MachineTier.WOOD, MapColor.WOOD, SoundType.WOOD, 2.5f);
    public static final DeferredBlock<MinerBlock> MINER_STONE =
            miner(MachineTier.STONE, MapColor.STONE, SoundType.STONE, 3.5f);
    public static final DeferredBlock<MinerBlock> MINER_IRON =
            miner(MachineTier.IRON, MapColor.METAL, SoundType.METAL, 4.5f);
    public static final DeferredBlock<MinerBlock> MINER_DIAMOND =
            miner(MachineTier.DIAMOND, MapColor.DIAMOND, SoundType.METAL, 5.5f);

    /** Every miner tier, in progression order. Used for registration and datagen. */
    public static final List<DeferredBlock<MinerBlock>> MINERS =
            List.of(MINER_WOOD, MINER_STONE, MINER_IRON, MINER_DIAMOND);

    private static DeferredBlock<MinerBlock> miner(
            MachineTier tier, MapColor colour, SoundType sound, float strength) {
        return BLOCKS.registerBlock(
                "miner_" + tier.id(),
                properties -> new MinerBlock(properties, tier),
                properties -> properties
                        .mapColor(colour)
                        .strength(strength)
                        .sound(sound));
    }

    private ModBlocks() {}
}
