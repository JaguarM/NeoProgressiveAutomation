package com.jaguarm.neoprogressiveautomation.registry;

import com.jaguarm.neoprogressiveautomation.NeoProgressiveAutomation;
import com.jaguarm.neoprogressiveautomation.machine.miner.MinerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, NeoProgressiveAutomation.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MinerMenu>> MINER =
            MENUS.register("miner", () -> new MenuType<>(MinerMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}
