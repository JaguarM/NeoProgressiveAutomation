package com.jaguarm.neoprogressiveautomation.machine.miner;

import com.jaguarm.neoprogressiveautomation.Config;
import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Draws the miner's dig area in the world while its screen is open.
 *
 * <p>Range is otherwise an invisible number: nothing tells you what a Range module actually
 * bought until you watch the machine dig for a while. Outlining the footprint makes the
 * effect legible immediately, the way Factorio shows a drill's coverage while placing it.
 */
@EventBusSubscriber(modid = NeoProgressiveAutomation.MODID, value = Dist.CLIENT)
public final class MinerAreaPreview {

    private static final int OUTLINE_ARGB = 0xCC4AC3E8;
    private static final float LINE_WIDTH = 2.0F;

    private MinerAreaPreview() {}

    @SubscribeEvent
    static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof MinerScreen screen) || minecraft.level == null) {
            return;
        }

        MinerMenu menu = screen.getMenu();
        AABB area = digArea(minecraft.level, menu.machinePos(), menu.range());
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;

        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.lines(),
                (pose, buffer) -> ShapeRenderer.renderShape(
                        poseStack,
                        buffer,
                        Shapes.create(area),
                        -camera.x,
                        -camera.y,
                        -camera.z,
                        OUTLINE_ARGB,
                        LINE_WIDTH));
    }

    /**
     * The volume the miner will clear: a square of side {@code 2 * range - 1} centred on the
     * machine, running from the block directly beneath it down to the configured floor.
     * Mirrors the bounds {@code MinerBlockEntity.findNextTarget} walks.
     */
    private static AABB digArea(Level level, BlockPos pos, int range) {
        int reach = range - 1;
        int floor = Math.max(Config.MINE_FLOOR.get(), level.getMinY());
        return new AABB(
                pos.getX() - reach,
                floor,
                pos.getZ() - reach,
                pos.getX() + reach + 1,
                pos.getY(),
                pos.getZ() + reach + 1);
    }
}
