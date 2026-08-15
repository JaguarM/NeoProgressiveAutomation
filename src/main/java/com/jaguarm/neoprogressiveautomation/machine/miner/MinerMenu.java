package com.jaguarm.neoprogressiveautomation.machine.miner;

import com.jaguarm.neoprogressiveautomation.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MinerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    /** Client-side constructor: the menu is opened with a stand-in container that the server syncs into. */
    public MinerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MinerBlockEntity.SLOT_COUNT),
                new SimpleContainerData(5));
    }

    public MinerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.MINER.get(), containerId);
        checkContainerSize(container, MinerBlockEntity.SLOT_COUNT);
        this.container = container;
        this.data = data;

        // Machine slots. Positions mirror the generated GUI texture.
        addSlot(new Slot(container, MinerBlockEntity.SLOT_COBBLE, 8, 17));
        addSlot(new Slot(container, MinerBlockEntity.SLOT_FUEL, 8, 53));
        addSlot(new Slot(container, MinerBlockEntity.SLOT_PICKAXE, 44, 17));
        addSlot(new Slot(container, MinerBlockEntity.SLOT_SHOVEL, 44, 35));
        addSlot(new Slot(container, MinerBlockEntity.SLOT_UPGRADE, 44, 53));

        // Output grid is extract-only.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = MinerBlockEntity.SLOT_OUTPUT_START + row * 3 + col;
                addSlot(new OutputSlot(container, index, 116 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    /** A slot the miner fills and the player may only take from. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    public boolean isBurning() {
        return data.get(0) > 0;
    }

    /** Why the machine is or is not running, for the status line in the screen. */
    public MinerStatus status() {
        return MinerStatus.byOrdinal(data.get(4));
    }

    /** Fraction of the current fuel item remaining, for the flame indicator. */
    public float fuelProgress() {
        int total = data.get(1);
        return total == 0 ? 0.0f : (float) data.get(0) / total;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int machineEnd = MinerBlockEntity.SLOT_COUNT;
        int inventoryEnd = machineEnd + 36;

        if (index < machineEnd) {
            // Machine -> player inventory
            if (!moveItemStackTo(stack, machineEnd, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> whichever machine slot will accept it
            if (!moveIntoMachine(stack)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * Routes a shift-clicked stack to the first machine slot that accepts it, so cobble
     * goes to the cobble slot and coal to the fuel slot without the player aiming.
     */
    private boolean moveIntoMachine(ItemStack stack) {
        int[] candidates = {
            MinerBlockEntity.SLOT_COBBLE,
            MinerBlockEntity.SLOT_FUEL,
            MinerBlockEntity.SLOT_PICKAXE,
            MinerBlockEntity.SLOT_SHOVEL,
            MinerBlockEntity.SLOT_UPGRADE
        };
        for (int slotIndex : candidates) {
            if (container.canPlaceItem(slotIndex, stack)) {
                // The machine slots are added in this same order, so the menu index matches.
                int menuIndex = menuIndexOf(slotIndex);
                if (moveItemStackTo(stack, menuIndex, menuIndex + 1, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int menuIndexOf(int containerSlot) {
        return switch (containerSlot) {
            case MinerBlockEntity.SLOT_COBBLE -> 0;
            case MinerBlockEntity.SLOT_FUEL -> 1;
            case MinerBlockEntity.SLOT_PICKAXE -> 2;
            case MinerBlockEntity.SLOT_SHOVEL -> 3;
            default -> 4;
        };
    }
}
