package org.mods.gd656killicon.fabric.server;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.fabricmc.api.ModInitializer;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.fabric.events.FabricServerEvents;
import org.mods.gd656killicon.server.command.ServerCommands;

public class FabricModEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Gd656killicon.bootstrap();
        FabricServerEvents.init();
        CommandRegistrationEvent.EVENT.register((dispatcher, buildContext, selection) -> ServerCommands.register(dispatcher));
    }
}
