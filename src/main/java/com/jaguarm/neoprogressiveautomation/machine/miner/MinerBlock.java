package com.jaguarm.neoprogressiveautomation.machine.miner;

import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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

    public static final MapCodec<MinerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MachineTier.CODEC.fieldOf("tier").forGetter(MinerBlock::tier),
            propertiesCodec()
    ).apply(instance, (tier, properties) -> new MinerBlock(properties, tier)));

    /** Lit while the machine is actually working, so the model can show an active face. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Which way the drill's face points. Cosmetic: it digs straight down regardless. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MachineTier tier;

    public MinerBlock(Properties properties, MachineTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(getStateDefinition().any()
                .setValue(LIT, false)
                .setValue(FACING, Direction.NORTH));
    }

    public MachineTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    /** Faces the player who placed it, the way a furnace does. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBlockEntity(pos, state);
    }

    /** Remember the placer, so the machine mines as them for protection purposes. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);
        if (level.getBlockEntity(pos) instanceof MinerBlockEntity miner) {
            miner.setOwner(placer);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MINER.get(), MinerBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MinerBlockEntity miner) {
                // Position variant: the client needs it to draw the dig-area preview.
                player.openMenu(miner, pos);
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
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
