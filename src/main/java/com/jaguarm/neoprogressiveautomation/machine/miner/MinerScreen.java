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

    public MinerScreen(MinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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
        graphics.text(this.font, status.label(), 8, 72, colour, false);
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
    }
}
