package com.jaguarm.neoprogressiveautomation;

import org.slf4j.Logger;

import com.jaguarm.neoprogressiveautomation.registry.ModBlockEntities;
import com.jaguarm.neoprogressiveautomation.registry.ModBlocks;
import com.jaguarm.neoprogressiveautomation.registry.ModItems;
import com.jaguarm.neoprogressiveautomation.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeoProgressiveAutomation.MODID)
public class NeoProgressiveAutomation {

    public static final String MODID = "neoprogressiveautomation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoProgressiveAutomation(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
