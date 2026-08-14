package org.mods.gd656killicon.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.mods.gd656killicon.client.gui.MainConfigScreen;

public class FabricModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MainConfigScreen(parent);
    }
}
