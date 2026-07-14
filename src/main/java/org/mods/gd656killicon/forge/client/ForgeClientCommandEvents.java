package org.mods.gd656killicon.forge.client;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.client.command.ClientCommand;

public final class ForgeClientCommandEvents {
    private ForgeClientCommandEvents() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommand.register(event.getDispatcher());
    }
}
