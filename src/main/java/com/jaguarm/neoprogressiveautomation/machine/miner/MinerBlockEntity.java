package com.jaguarm.neoprogressiveautomation.machine.miner;

import java.util.List;

import com.jaguarm.neoprogressiveautomation.Config;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.Spiral;
import com.jaguarm.neoprogressiveautomation.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The miner digs a square spiral of columns centred on itself, each column running from
 * the block directly beneath the machine down to the configured floor.
 *
 * <p>It needs solid fuel, a pickaxe and a shovel, and — unless disabled in the config —
 * one cobblestone per block mined, which it uses to backfill the hole. Tools take
 * durability damage per block and respect Silk Touch, Fortune and Efficiency.
 */
public class MinerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_COBBLE = 1;
    public static final int SLOT_PICKAXE = 2;
    public static final int SLOT_SHOVEL = 3;
    public static final int SLOT_UPGRADE = 4;
    public static final int SLOT_OUTPUT_START = 5;
    public static final int OUTPUT_SLOTS = 9;
    public static final int SLOT_COUNT = SLOT_OUTPUT_START + OUTPUT_SLOTS;

    /** Which tool, if any, is needed for the block currently being dug. */
    private enum ToolChoice {
        /** Cannot be mined with the equipment present. */
        NONE,
        /** Breakable by hand, no tool damage. */
        HAND,
        PICKAXE,
        SHOVEL
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final MachineTier tier;

    private int burnTime;
    private int burnTimeTotal;

    /** 1-based spiral index of the column currently being dug. */
    private int columnIndex = 1;
    private int currentY = Integer.MIN_VALUE;
    private int elapsedTicks;
    private int requiredTicks;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MINER.get(), pos, state, MachineTier.WOOD);
    }

    protected MinerBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            MachineTier tier) {
        super(type, pos, state);
        this.tier = tier;
    }

    public MachineTier tier() {
        return tier;
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // -- Ticking ----------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MinerBlockEntity miner) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        miner.tick(serverLevel);
    }

    private void tick(ServerLevel level) {
        if (currentY == Integer.MIN_VALUE) {
            currentY = worldPosition.getY() - 1;
        }

        boolean changed = false;

        if (burnTime > 0) {
            burnTime--;
            changed = true;
        }

        if (hasRequiredEquipment() && (burnTime > 0 || tryConsumeFuel(level))) {
            if (advanceMining(level)) {
                changed = true;
            }
        }

        syncLitState(level);

        if (changed) {
            setChanged();
        }
    }

    /** Keeps the LIT blockstate in step with the fuel, so the block model shows activity. */
    private void syncLitState(ServerLevel level) {
        BlockState state = getBlockState();
        boolean lit = isBurning();
        if (state.hasProperty(MinerBlock.LIT) && state.getValue(MinerBlock.LIT) != lit) {
            level.setBlockAndUpdate(worldPosition, state.setValue(MinerBlock.LIT, lit));
        }
    }

    /** The miner refuses to run without both tools, and without cobble when backfilling. */
    private boolean hasRequiredEquipment() {
        if (items.get(SLOT_PICKAXE).isEmpty() || items.get(SLOT_SHOVEL).isEmpty()) {
            return false;
        }
        return !Config.REQUIRE_COBBLE_BACKFILL.get() || !items.get(SLOT_COBBLE).isEmpty();
    }

    private boolean tryConsumeFuel(ServerLevel level) {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.isEmpty()) {
            return false;
        }
        int itemBurnTime = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (itemBurnTime <= 0) {
            return false;
        }
        burnTime = itemBurnTime;
        burnTimeTotal = itemBurnTime;
        fuel.shrink(1);
        return true;
    }

    // -- Mining -----------------------------------------------------------------

    /** @return true if any state changed and the block entity needs saving */
    private boolean advanceMining(ServerLevel level) {
        BlockPos target = findNextTarget(level);
        if (target == null) {
            return false;
        }

        BlockState state = level.getBlockState(target);
        ToolChoice tool = toolFor(state);
        if (tool == ToolChoice.NONE) {
            return false;
        }

        int needed = miningDuration(state, tool);
        if (requiredTicks != needed) {
            // The target changed under us (another miner, a player, a falling block).
            // Re-time rather than breaking a block we never spent the ticks on.
            requiredTicks = needed;
            elapsedTicks = 0;
            return true;
        }

        if (elapsedTicks < requiredTicks) {
            elapsedTicks++;
            return true;
        }

        mineBlock(level, target, state, tool);
        elapsedTicks = 0;
        requiredTicks = 0;
        return true;
    }

    /**
     * Walks down the current column, then outward along the spiral, until it finds a
     * block this miner can actually break. Returns null when the whole area is done.
     */
    private BlockPos findNextTarget(ServerLevel level) {
        int floor = Math.max(Config.MINE_FLOOR.get(), level.getMinY());
        int totalColumns = Spiral.columnsForRadius(range());

        while (columnIndex <= totalColumns) {
            Spiral.Offset offset = Spiral.offset(columnIndex);
            BlockPos column = worldPosition.offset(offset.x(), 0, offset.z());

            while (currentY >= floor) {
                BlockPos candidate = new BlockPos(column.getX(), currentY, column.getZ());
                if (toolFor(level.getBlockState(candidate)) != ToolChoice.NONE) {
                    return candidate;
                }
                currentY--;
            }

            columnIndex++;
            currentY = worldPosition.getY() - 1;
        }
        return null;
    }

    /** Decides which tool breaks this block, or NONE if the miner cannot handle it. */
    private ToolChoice toolFor(BlockState state) {
        if (state.isAir()) {
            return ToolChoice.NONE;
        }
        // Never chew through bedrock or other unbreakable blocks.
        if (state.getDestroySpeed(level, worldPosition) < 0) {
            return ToolChoice.NONE;
        }
        // Skip liquids; the original treated them as unmineable without a filler upgrade.
        if (!state.getFluidState().isEmpty()) {
            return ToolChoice.NONE;
        }
        // Don't re-mine our own backfill.
        if (state.is(Blocks.COBBLESTONE) && Config.REQUIRE_COBBLE_BACKFILL.get()) {
            return ToolChoice.NONE;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return items.get(SLOT_PICKAXE).isCorrectToolForDrops(state) ? ToolChoice.PICKAXE : ToolChoice.NONE;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return items.get(SLOT_SHOVEL).isCorrectToolForDrops(state) ? ToolChoice.SHOVEL : ToolChoice.NONE;
        }
        return ToolChoice.HAND;
    }

    /**
     * Mirrors vanilla's break timing: hardness scaled to ticks, divided by the tool's dig
     * speed, with Efficiency applying the same 1.3x per level the original mod used.
     */
    private int miningDuration(BlockState state, ToolChoice tool) {
        int base = (int) Math.ceil(state.getDestroySpeed(level, worldPosition) * 1.5 * 20);
        if (tool == ToolChoice.HAND) {
            return Math.max(base, 1);
        }

        ItemStack toolStack = items.get(slotFor(tool));
        float speed = toolStack.getDestroySpeed(state);
        if (speed <= 1.0f) {
            return Math.max(base, 1);
        }

        int efficiency = enchantmentLevel(toolStack, Enchantments.EFFICIENCY);
        for (int i = 0; i < efficiency; i++) {
            speed *= 1.3f;
        }
        return Math.max((int) Math.ceil(base / speed), 1);
    }

    private void mineBlock(ServerLevel level, BlockPos target, BlockState state, ToolChoice tool) {
        BlockEntity targetEntity = level.getBlockEntity(target);
        ItemStack toolStack = tool == ToolChoice.HAND ? ItemStack.EMPTY : items.get(slotFor(tool));

        List<ItemStack> drops;
        if (enchantmentLevel(toolStack, Enchantments.SILK_TOUCH) > 0) {
            drops = List.of(new ItemStack(state.getBlock()));
        } else {
            drops = Block.getDrops(state, level, target, targetEntity, null, toolStack);
        }
        for (ItemStack drop : drops) {
            storeOrDrop(level, drop);
        }

        if (Config.REQUIRE_COBBLE_BACKFILL.get()) {
            level.setBlockAndUpdate(target, Blocks.COBBLESTONE.defaultBlockState());
            items.get(SLOT_COBBLE).shrink(1);
        } else {
            level.removeBlock(target, false);
        }

        if (tool != ToolChoice.HAND) {
            damageTool(level, slotFor(tool));
        }
    }

    private void damageTool(ServerLevel level, int slot) {
        ItemStack toolStack = items.get(slot);
        toolStack.hurtAndBreak(1, level, null, item -> {
            if (Config.DESTROY_TOOLS.get()) {
                items.set(slot, ItemStack.EMPTY);
            }
        });
    }

    /** Puts a stack in the output slots, dropping any remainder into the world. */
    private void storeOrDrop(ServerLevel level, ItemStack stack) {
        for (int i = SLOT_OUTPUT_START; i < SLOT_COUNT && !stack.isEmpty(); i++) {
            ItemStack slotStack = items.get(i);
            if (slotStack.isEmpty()) {
                items.set(i, stack.copyAndClear());
                return;
            }
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int room = slotStack.getMaxStackSize() - slotStack.getCount();
                int moved = Math.min(room, stack.getCount());
                slotStack.grow(moved);
                stack.shrink(moved);
            }
        }
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition.above(), stack);
        }
    }

    private int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || level == null) {
            return 0;
        }
        Holder<Enchantment> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    private static int slotFor(ToolChoice tool) {
        return tool == ToolChoice.PICKAXE ? SLOT_PICKAXE : SLOT_SHOVEL;
    }

    /** Radius in blocks, from the base config plus however many upgrades are installed. */
    public int range() {
        int upgrades = Math.min(items.get(SLOT_UPGRADE).getCount(), tier.maxRangeUpgrades());
        return Config.INITIAL_RANGE.get() + upgrades * Config.UPGRADE_RANGE.get();
    }

    // -- Container --------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    /** Keeps players from dropping a pickaxe into the fuel slot and wondering why nothing burns. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> level != null && stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
            case SLOT_COBBLE -> stack.is(Blocks.COBBLESTONE.asItem());
            case SLOT_PICKAXE -> stack.is(ItemTags.PICKAXES);
            case SLOT_SHOVEL -> stack.is(ItemTags.SHOVELS);
            case SLOT_UPGRADE -> false; // no upgrade items exist yet
            default -> false; // output slots are extract-only
        };
    }

    // -- MenuProvider -----------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.neoprogressiveautomation.miner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MinerMenu(containerId, inventory, this, dataAccess);
    }

    /** Syncs burn progress to the client so the screen can draw a fuel indicator. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTimeTotal;
                case 2 -> elapsedTicks;
                case 3 -> requiredTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnTimeTotal = value;
                case 2 -> elapsedTicks = value;
                case 3 -> requiredTicks = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    // -- Persistence ------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnTimeTotal", burnTimeTotal);
        output.putInt("ColumnIndex", columnIndex);
        output.putInt("CurrentY", currentY);
        output.putInt("ElapsedTicks", elapsedTicks);
        output.putInt("RequiredTicks", requiredTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        burnTimeTotal = input.getIntOr("BurnTimeTotal", 0);
        columnIndex = input.getIntOr("ColumnIndex", 1);
        currentY = input.getIntOr("CurrentY", Integer.MIN_VALUE);
        elapsedTicks = input.getIntOr("ElapsedTicks", 0);
        requiredTicks = input.getIntOr("RequiredTicks", 0);
    }
}
