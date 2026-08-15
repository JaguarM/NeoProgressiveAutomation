package com.jaguarm.neoprogressiveautomation;

import com.jaguarm.neoprogressiveautomation.machine.miner.MinerScreen;
import com.jaguarm.neoprogressiveautomation.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = NeoProgressiveAutomation.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoProgressiveAutomation.MODID, value = Dist.CLIENT)
public class NeoProgressiveAutomationClient {

    public NeoProgressiveAutomationClient(ModContainer container) {
        // Lets NeoForge build a config screen for this mod, reachable from the Mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MINER.get(), MinerScreen::new);
    }
}
