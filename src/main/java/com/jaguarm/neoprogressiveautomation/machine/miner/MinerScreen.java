package com.jaguarm.neoprogressiveautomation.machine.miner;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MinerScreen extends AbstractContainerScreen<MinerMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            NeoProgressiveAutomation.MODID, "textures/gui/container/miner.png");

    /** Dark grey, matching the vanilla container label colour. */
    private static final int COLOUR_OK = -12566464;
    /** Muted red, for a machine that is waiting on the player. */
    private static final int COLOUR_BLOCKED = 0xFF9C2A2A;

    /** Semi-transparent black over module slots this tier cannot use. */
    private static final int LOCKED_SLOT_OVERLAY = 0xA0101010;

    /** Must match MinerMenu's module slot placement. */
    private static final int MODULE_SLOT_X = 44;
    private static final int MODULE_SLOT_Y = 53;

    /** 18px taller than the vanilla 166, leaving a clear row for the status line. */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 184;

    public MinerScreen(MinerMenu menu, Inventory inventory, Component title) {
        // Passing the size here rather than assigning afterwards: imageHeight is final, and
        // this constructor also derives inventoryLabelY from it, so the "Inventory" label
        // moves down with the panel instead of colliding with the status line.
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        MinerStatus status = this.menu.status();
        int colour = status == MinerStatus.RUNNING || status == MinerStatus.COMPLETE
                ? COLOUR_OK
                : COLOUR_BLOCKED;
        graphics.text(this.font, status.label(), 8, 73, colour, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                x, y,
                0.0F, 0.0F,
                this.imageWidth, this.imageHeight,
                256, 256);

        shadeLockedModuleSlots(graphics, x, y);
    }

    /**
     * Darkens module slots this tier has not unlocked.
     *
     * <p>Every miner draws all three slots so the layout is stable across tiers, but a
     * wooden miner unlocks none of them. Without shading those read as ordinary empty
     * slots that silently refuse everything.
     */
    private void shadeLockedModuleSlots(GuiGraphicsExtractor graphics, int originX, int originY) {
        for (int i = 0; i < MinerBlockEntity.MODULE_SLOTS; i++) {
            if (this.menu.isModuleSlotUnlocked(i)) {
                continue;
            }
            int slotX = originX + MODULE_SLOT_X + i * 18;
            int slotY = originY + MODULE_SLOT_Y;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, LOCKED_SLOT_OVERLAY);
        }
    }
}
