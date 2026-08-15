package com.jaguarm.neoprogressiveautomation.machine.miner;

import com.jaguarm.neoprogressiveautomation.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class MinerBlock extends BaseEntityBlock {

    public static final MapCodec<MinerBlock> CODEC = simpleCodec(MinerBlock::new);

    /** Lit while the machine has fuel burning, so the model can show an active face. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MinerBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MINER.get(), MinerBlockEntity::serverTick);
    }

    /** Spill the machine's contents when it is broken, so tools and ore are not lost. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MinerBlockEntity miner) {
            Containers.dropContents(level, pos, miner.items());
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
