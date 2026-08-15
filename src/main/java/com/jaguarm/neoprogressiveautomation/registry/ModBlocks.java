package com.jaguarm.neoprogressiveautomation.registry;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NeoProgressiveAutomation.MODID);

    public static final DeferredBlock<MinerBlock> MINER = BLOCKS.registerBlock(
            "miner",
            MinerBlock::new,
            properties -> properties
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD));

    private ModBlocks() {}
}
