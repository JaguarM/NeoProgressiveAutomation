package com.jaguarm.neoprogressiveautomation.machine.miner;

import java.util.List;

import com.jaguarm.neoprogressiveautomation.Config;
import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.ModuleItem;
import com.jaguarm.neoprogressiveautomation.machine.ModuleType;
import com.jaguarm.neoprogressiveautomation.machine.Spiral;
import com.jaguarm.neoprogressiveautomation.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.WorldlyContainer;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The miner digs a square spiral of columns centred on itself, each column running from
 * the block directly beneath the machine down to the configured floor.
 *
 * <p>It needs solid fuel, a pickaxe and a shovel, and — unless disabled in the config —
 * one cobblestone per block mined, which it uses to backfill the hole. Tools take
 * durability damage per block and respect Silk Touch, Fortune and Efficiency.
 */
public class MinerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_COBBLE = 1;
    public static final int SLOT_PICKAXE = 2;
    public static final int SLOT_SHOVEL = 3;
    /** Module slots are always allocated; the tier decides how many are unlocked. */
    public static final int SLOT_MODULE_START = 4;
    public static final int MODULE_SLOTS = MachineTier.MAX_MODULE_SLOTS;
    public static final int SLOT_OUTPUT_START = SLOT_MODULE_START + MODULE_SLOTS;
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
    private MinerStatus status = MinerStatus.NO_PICKAXE;

    /** How often the miner scans its neighbours to push output. */
    private static final int PUSH_INTERVAL_TICKS = 20;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER.get(), pos, state);
        // One block entity type backs all four miner blocks; the tier comes from whichever
        // block this entity was placed for.
        this.tier = state.getBlock() instanceof MinerBlock miner ? miner.tier() : MachineTier.WOOD;
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

        MinerStatus newStatus = runMining(level);
        if (newStatus != status) {
            NeoProgressiveAutomation.LOGGER.debug(
                    "miner@{} status {} -> {} (column {}, y {})",
                    worldPosition, status, newStatus, columnIndex, currentY);
            status = newStatus;
            changed = true;
        }

        if (pushOutput(level)) {
            changed = true;
        }

        syncLitState(level);

        if (changed) {
            setChanged();
        }
    }

    /**
     * Pushes mined items into any adjacent inventory, so a chest beside the miner is
     * enough and no hopper is needed.
     *
     * <p>Throttled rather than run every tick: a full scan of six neighbours 20 times a
     * second is wasted work when the machine produces a block every few seconds at best.
     *
     * @return true if anything moved
     */
    private boolean pushOutput(ServerLevel level) {
        if (level.getGameTime() % PUSH_INTERVAL_TICKS != 0) {
            return false;
        }

        boolean moved = false;
        for (Direction direction : Direction.values()) {
            if (isOutputEmpty()) {
                break;
            }
            ResourceHandler<ItemResource> target = level.getCapability(
                    Capabilities.Item.BLOCK,
                    worldPosition.relative(direction),
                    direction.getOpposite());
            if (target == null) {
                continue;
            }
            moved |= pushInto(target);
        }
        return moved;
    }

    private boolean pushInto(ResourceHandler<ItemResource> target) {
        boolean moved = false;
        for (int slot = SLOT_OUTPUT_START; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // Transactional insert: nothing leaves this machine unless the destination
            // actually took it, so a full chest cannot void the stack.
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = target.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted <= 0) {
                    continue;
                }
                transaction.commit();
                stack.shrink(inserted);
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                moved = true;
            }
        }
        return moved;
    }

    private boolean isOutputEmpty() {
        for (int slot = SLOT_OUTPUT_START; slot < SLOT_COUNT; slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Runs one step of mining and reports why it could or could not proceed. Every early
     * return is a distinct reason so the GUI can tell the player what is missing rather
     * than sitting silently idle.
     */
    private MinerStatus runMining(ServerLevel level) {
        if (items.get(SLOT_PICKAXE).isEmpty()) {
            return MinerStatus.NO_PICKAXE;
        }
        if (items.get(SLOT_SHOVEL).isEmpty()) {
            return MinerStatus.NO_SHOVEL;
        }
        if (Config.REQUIRE_COBBLE_BACKFILL.get() && items.get(SLOT_COBBLE).isEmpty()) {
            return MinerStatus.NO_COBBLE;
        }
        if (burnTime == 0 && !tryConsumeFuel(level)) {
            return MinerStatus.NO_FUEL;
        }

        BlockPos target = findNextTarget(level);
        if (target == null) {
            return MinerStatus.COMPLETE;
        }

        advanceMining(level, target);
        traceProgress(level, target);
        return MinerStatus.RUNNING;
    }

    /**
     * Periodic trace of what the miner is actually chewing on. A machine that looks stuck
     * from the surface is usually working a column you cannot see, so log the column index
     * and target rather than guessing.
     */
    private void traceProgress(ServerLevel level, BlockPos target) {
        if (level.getGameTime() % 40 != 0) {
            return;
        }
        NeoProgressiveAutomation.LOGGER.debug(
                "miner@{} column {}/{} target {} block {} elapsed {}/{} burn {}",
                worldPosition,
                columnIndex,
                Spiral.columnsForRadius(range()),
                target,
                level.getBlockState(target).getBlock().getName().getString(),
                elapsedTicks,
                requiredTicks,
                burnTime);
    }

    /** Keeps the LIT blockstate in step with the fuel, so the block model shows activity. */
    private void syncLitState(ServerLevel level) {
        BlockState state = getBlockState();
        boolean lit = isBurning();
        if (state.hasProperty(MinerBlock.LIT) && state.getValue(MinerBlock.LIT) != lit) {
            level.setBlockAndUpdate(worldPosition, state.setValue(MinerBlock.LIT, lit));
        }
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
        int scaled = scaledBurnTime(itemBurnTime);
        burnTime = scaled;
        burnTimeTotal = scaled;
        fuel.shrink(1);
        return true;
    }

    // -- Mining -----------------------------------------------------------------

    private void advanceMining(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        ToolChoice tool = toolFor(state, target);
        if (tool == ToolChoice.NONE) {
            return;
        }

        int needed = miningDuration(state, target, tool);
        if (requiredTicks != needed) {
            // The target changed under us (another miner, a player, a falling block).
            // Re-time rather than breaking a block we never spent the ticks on.
            requiredTicks = needed;
            elapsedTicks = 0;
            return;
        }

        if (elapsedTicks < requiredTicks) {
            elapsedTicks++;
            return;
        }

        mineBlock(level, target, state, tool);
        elapsedTicks = 0;
        requiredTicks = 0;
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
                if (toolFor(level.getBlockState(candidate), candidate) != ToolChoice.NONE) {
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
    private ToolChoice toolFor(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return ToolChoice.NONE;
        }
        // Never chew through bedrock or other unbreakable blocks.
        if (state.getDestroySpeed(level, pos) < 0) {
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
    private int miningDuration(BlockState state, BlockPos pos, ToolChoice tool) {
        int base = (int) Math.ceil(state.getDestroySpeed(level, pos) * 1.5 * 20);
        if (tool == ToolChoice.HAND) {
            return Math.max(Math.round(base * miningTimeFactor()), 1);
        }

        ItemStack toolStack = items.get(slotFor(tool));
        float speed = toolStack.getDestroySpeed(state);
        if (speed <= 1.0f) {
            return Math.max(Math.round(base * miningTimeFactor()), 1);
        }

        int efficiency = enchantmentLevel(toolStack, Enchantments.EFFICIENCY);
        for (int i = 0; i < efficiency; i++) {
            speed *= 1.3f;
        }
        return Math.max((int) Math.ceil(base / speed * miningTimeFactor()), 1);
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

    /**
     * How many modules of a type are installed in unlocked slots.
     *
     * <p>Counts stack sizes, not slots, and always re-checks that the slot is unlocked, so
     * a module that reached a locked slot by some route other than canPlaceItem still has
     * no effect.
     */
    private int moduleCount(ModuleType type) {
        int count = 0;
        for (int i = 0; i < MODULE_SLOTS; i++) {
            if (!tier.hasModuleSlot(i)) {
                continue;
            }
            ItemStack stack = items.get(SLOT_MODULE_START + i);
            if (ModuleItem.typeOf(stack) == type) {
                // One module per slot, Factorio-style. Counting stack sizes let three
                // slots of 64 range modules reach radius 193, which is 148k columns and
                // effectively unbounded. The menu caps the slot; this caps the effect,
                // so a stack arriving by any other route still cannot exceed one.
                count++;
            }
        }
        return count;
    }

    /** Radius in blocks: the configured base, widened by range modules. */
    public int range() {
        int bonus = moduleCount(ModuleType.RANGE) * ModuleType.RANGE.bonusRadius();
        return Config.INITIAL_RANGE.get() + bonus * Config.UPGRADE_RANGE.get();
    }

    /** Compounded module factor, e.g. 0.8^n for n speed modules. */
    private float moduleFactor(ModuleType type, boolean forFuel) {
        float per = forFuel ? type.fuelUseFactor() : type.miningTimeFactor();
        return (float) Math.pow(per, moduleCount(type));
    }

    /** Mining time multiplier from speed and efficiency modules combined. */
    private float miningTimeFactor() {
        return moduleFactor(ModuleType.SPEED, false) * moduleFactor(ModuleType.EFFICIENCY, false);
    }

    /**
     * How long one fuel item lasts, after modules.
     *
     * <p>Scaled here rather than by draining more per tick: burn time is an int, and
     * rounding a 1.4x drain back to 1 would silently discard the whole speed-module
     * penalty. Dividing the item's burn time keeps the trade-off exact.
     */
    private int scaledBurnTime(int itemBurnTime) {
        float factor = moduleFactor(ModuleType.SPEED, true) * moduleFactor(ModuleType.EFFICIENCY, true);
        return Math.max(1, Math.round(itemBurnTime / factor));
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
        return acceptsInSlot(slot, stack, level, tier.moduleSlots());
    }

    /**
     * Whether a slot accepts a stack, as a pure function of the stack.
     *
     * <p>Static and container-independent on purpose. The client's menu is backed by a
     * plain SimpleContainer, which accepts anything, so a rule that consulted the container
     * would say "yes" on the client and "no" on the server. Shift-clicking then visibly
     * dropped the item into the first slot for a frame before the server corrected it.
     * Both sides now evaluate the same rule and agree immediately.
     *
     * @param unlockedModuleSlots how many module slots the machine's tier allows; the
     *                            client reads this from synced container data
     */
    public static boolean acceptsInSlot(int slot, ItemStack stack, @Nullable Level level, int unlockedModuleSlots) {
        return switch (slot) {
            case SLOT_FUEL -> level != null && stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
            case SLOT_COBBLE -> stack.is(Blocks.COBBLESTONE.asItem());
            case SLOT_PICKAXE -> stack.is(ItemTags.PICKAXES);
            case SLOT_SHOVEL -> stack.is(ItemTags.SHOVELS);
            default -> {
                int moduleIndex = slot - SLOT_MODULE_START;
                yield moduleIndex >= 0
                        && moduleIndex < MODULE_SLOTS
                        && moduleIndex < unlockedModuleSlots
                        && ModuleItem.typeOf(stack) != null;
            }
        };
    }

    // -- Automation faces -------------------------------------------------------

    private static final int[] OUTPUT_SLOTS_BY_INDEX =
            java.util.stream.IntStream.range(SLOT_OUTPUT_START, SLOT_COUNT).toArray();
    private static final int[] INPUT_SLOTS_BY_INDEX = java.util.stream.IntStream
            .concat(
                    java.util.stream.IntStream.of(SLOT_FUEL, SLOT_COBBLE, SLOT_PICKAXE, SLOT_SHOVEL),
                    java.util.stream.IntStream.range(SLOT_MODULE_START, SLOT_OUTPUT_START))
            .toArray();

    /**
     * A hopper underneath should drain the mined output, not steal the fuel.
     *
     * <p>Without this, automation sees a flat 14-slot container and works left to right
     * from slot 0, which is the fuel slot. Exposing only the output grid downward and only
     * the input slots elsewhere makes the obvious setup behave the way it looks.
     */
    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS_BY_INDEX : INPUT_SLOTS_BY_INDEX;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        // Only finished product leaves the machine. Fuel, cobble and tools stay put,
        // otherwise a hopper would strip the pickaxe straight back out again.
        return slot >= SLOT_OUTPUT_START;
    }

    // -- MenuProvider -----------------------------------------------------------

    @Override
    public Component getDisplayName() {
        // Use the block's own name so the GUI title says "Stone Miner", not a generic label.
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MinerMenu(containerId, inventory, this, dataAccess, worldPosition);
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
                case 4 -> status.ordinal();
                case 5 -> tier.moduleSlots();
                case 6 -> range();
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
                case 4 -> status = MinerStatus.byOrdinal(value);
                // index 5 (module slot count) is derived from the block, never assigned
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 7;
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
