package com.jaguarm.neoprogressiveautomation.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.ModuleItem;
import com.jaguarm.neoprogressiveautomation.machine.ModuleType;
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

    /** Block items for each miner tier, in progression order. */
    public static final Map<MachineTier, DeferredItem<BlockItem>> MINERS = new LinkedHashMap<>();

    /** Modules are universal: one item per type, valid in any machine that has a free slot. */
    public static final Map<ModuleType, DeferredItem<ModuleItem>> MODULES = new LinkedHashMap<>();

    static {
        for (DeferredBlock<MinerBlock> block : ModBlocks.MINERS) {
            MINERS.put(tierOf(block), ITEMS.registerSimpleBlockItem(block));
        }
        for (ModuleType type : ModuleType.values()) {
            MODULES.put(type, ITEMS.registerItem(
                    type.id() + "_module",
                    properties -> new ModuleItem(properties, type)));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.neoprogressiveautomation"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> MINERS.get(MachineTier.WOOD).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        MINERS.values().forEach(item -> output.accept(item.get()));
                        MODULES.values().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    /**
     * The tier a miner block was registered for, read from its registry name. Block
     * suppliers cannot be resolved during registration, so the name is the only handle.
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
