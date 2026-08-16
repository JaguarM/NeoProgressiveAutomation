package com.jaguarm.neoprogressiveautomation.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.MachineTier;
import com.jaguarm.neoprogressiveautomation.machine.ModuleItem;
import com.jaguarm.neoprogressiveautomation.machine.ModuleType;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NeoProgressiveAutomation.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NeoProgressiveAutomation.MODID);

    /** Block items for the drills, keyed by tier. */
    public static final Map<MachineTier, DeferredItem<BlockItem>> DRILLS = new LinkedHashMap<>();

    /** Modules are universal: one item per type, valid in any machine with a free slot. */
    public static final Map<ModuleType, DeferredItem<ModuleItem>> MODULES = new LinkedHashMap<>();

    static {
        ModBlocks.DRILLS.forEach((tier, block) -> DRILLS.put(tier, ITEMS.registerSimpleBlockItem(block)));
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
                    .icon(() -> DRILLS.get(MachineTier.BURNER).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        DRILLS.values().forEach(item -> output.accept(item.get()));
                        MODULES.values().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModItems() {}
}
