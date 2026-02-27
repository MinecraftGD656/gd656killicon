package org.mods.gd656killicon.server;

import org.mods.gd656killicon.server.logic.core.BonusEngine;
import org.mods.gd656killicon.server.logic.core.ComboTracker;
import org.mods.gd656killicon.server.logic.core.CritTracker;
import org.mods.gd656killicon.server.logic.integration.PingWheelIntegration;
import org.mods.gd656killicon.server.logic.integration.SpottingIntegration;
import org.mods.gd656killicon.server.logic.integration.SuperbWarfareIntegration;
import org.mods.gd656killicon.server.logic.integration.TaczIntegration;
import org.mods.gd656killicon.server.logic.integration.YwzjVehicleIntegration;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ServerCore {
    public static final BonusEngine BONUS = new BonusEngine();
    public static final ComboTracker COMBO = new ComboTracker();
    public static final CritTracker CRIT = new CritTracker();
    public static final TaczIntegration TACZ = TaczIntegration.get();
    public static final SuperbWarfareIntegration SUPERB_WARFARE = SuperbWarfareIntegration.get();
    public static final YwzjVehicleIntegration YWZJ_VEHICLE = YwzjVehicleIntegration.get();
    public static final SpottingIntegration SPOTTING = SpottingIntegration.get();
    public static final PingWheelIntegration PING_WHEEL = PingWheelIntegration.get();
    public static final org.mods.gd656killicon.server.logic.integration.ImmersiveAircraftIntegration IMMERSIVE_AIRCRAFT = org.mods.gd656killicon.server.logic.integration.ImmersiveAircraftIntegration.get();

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
