package com.jaguarm.neoprogressiveautomation.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.UpgradeItem;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NeoProgressiveAutomation.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NeoProgressiveAutomation.MODID);

    /** Block items for each miner tier, keyed by tier so the creative tab can order them. */
    public static final Map<MachineTier, DeferredItem<BlockItem>> MINERS = new LinkedHashMap<>();

    /** One range upgrade per tier. A machine only accepts the upgrade matching its own tier. */
    public static final Map<MachineTier, DeferredItem<UpgradeItem>> RANGE_UPGRADES = new LinkedHashMap<>();

    static {
        for (DeferredBlock<MinerBlock> block : ModBlocks.MINERS) {
            MachineTier tier = tierOf(block);
            MINERS.put(tier, ITEMS.registerSimpleBlockItem(block));
            RANGE_UPGRADES.put(tier, ITEMS.registerItem(
                    tier.id() + "_upgrade",
                    properties -> new UpgradeItem(properties, tier)));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.neoprogressiveautomation"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> MINERS.get(MachineTier.WOOD).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        MINERS.values().forEach(item -> output.accept(item.get()));
                        RANGE_UPGRADES.values().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    /**
     * The tier a miner block was registered for. Read from the registry name rather than
     * the block instance, because block suppliers cannot be resolved during registration.
     */
    private static MachineTier tierOf(DeferredBlock<MinerBlock> block) {
        String path = block.getId().getPath();
        String suffix = path.substring(path.lastIndexOf('_') + 1);
        for (MachineTier tier : MachineTier.values()) {
            if (tier.id().equals(suffix)) {
                return tier;
            }
        }
        throw new IllegalStateException("No tier for miner block " + path);
    }

    private ModItems() {}
}
