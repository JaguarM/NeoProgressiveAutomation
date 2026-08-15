package com.jaguarm.neoprogressiveautomation.registry;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NeoProgressiveAutomation.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerBlockEntity>> MINER =
            BLOCK_ENTITIES.register(
                    "miner",
                    () -> new BlockEntityType<>(MinerBlockEntity::new, ModBlocks.MINER.get()));

    private ModBlockEntities() {}
}
