package org.mods.gd656killicon.forge.server;

import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;

@Mod(Gd656killicon.MODID)
public class ForgeModEntrypoint {
    public ForgeModEntrypoint() {
        Gd656killicon.bootstrap();
    }
}
