package org.mods.gd656killicon.forge.server;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.server.command.ServerCommands;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID)
public final class ForgeServerCommandEvents {
    private ForgeServerCommandEvents() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        ServerCommands.register(event.getDispatcher());
    }
}
