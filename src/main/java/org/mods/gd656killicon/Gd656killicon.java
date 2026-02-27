package org.mods.gd656killicon;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.mods.gd656killicon.network.NetworkHandler;

@Mod(Gd656killicon.MODID)
public class Gd656killicon {
    public static final String MODID = "gd656killicon";

    @SuppressWarnings({"removal"})
    public Gd656killicon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        NetworkHandler.register();

        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> org.mods.gd656killicon.client.ClientSetup.registerConfigScreen());

        MinecraftForge.EVENT_BUS.register(this);
    }
}
