package com.jaguarm.neoprogressiveautomation.registry;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;

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

    public static final DeferredItem<BlockItem> MINER = ITEMS.registerSimpleBlockItem(ModBlocks.MINER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.neoprogressiveautomation"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> MINER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(MINER.get()))
                    .build());

    private ModItems() {}
}
