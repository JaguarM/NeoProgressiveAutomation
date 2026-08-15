package com.jaguarm.neoprogressiveautomation.machine;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A range upgrade. Each one widens the area a machine works over.
 *
 * <p>Upgrades are keyed to a tier and a machine only accepts its own: a wooden miner takes
 * wooden upgrades and rejects diamond ones. That is deliberate and matches the original
 * mod — upgrades are not a universal currency, they are part of the tier progression, so
 * moving up a tier means rebuilding the upgrade stack too.
 */
public class UpgradeItem extends Item {

    private final MachineTier tier;

    public UpgradeItem(Properties properties, MachineTier tier) {
        super(properties);
        this.tier = tier;
    }

    public MachineTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("tooltip.neoprogressiveautomation.upgrade.tier",
                        Component.translatable("tooltip.neoprogressiveautomation.tier." + tier.id()))
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.neoprogressiveautomation.upgrade.max", tier.maxRangeUpgrades())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The upgrade tier of a stack, or null if it is not an upgrade at all. */
    public static MachineTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem upgrade ? upgrade.tier() : null;
    }
}
