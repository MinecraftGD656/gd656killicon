package org.mods.gd656killicon.server;

import org.mods.gd656killicon.server.logic.core.BonusEngine;
import org.mods.gd656killicon.server.logic.core.ComboTracker;
import org.mods.gd656killicon.server.logic.core.CritTracker;
import org.mods.gd656killicon.server.logic.integration.PingWheelIntegration;
import org.mods.gd656killicon.server.logic.integration.SpottingIntegration;
import org.mods.gd656killicon.server.logic.integration.SuperbWarfareIntegration;
import org.mods.gd656killicon.server.logic.integration.TaczIntegration;
import org.mods.gd656killicon.server.logic.integration.CustomNpcsIntegration;
import org.mods.gd656killicon.server.logic.integration.ConquestBattlefieldIntegration;
import org.mods.gd656killicon.server.logic.integration.YwzjVehicleIntegration;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import net.minecraft.server.MinecraftServer;

public class ServerCore {
    public static final BonusEngine BONUS = new BonusEngine();
    public static final ComboTracker COMBO = new ComboTracker();
    public static final CritTracker CRIT = new CritTracker();
    public static final TaczIntegration TACZ = TaczIntegration.get();
    public static final SuperbWarfareIntegration SUPERB_WARFARE = SuperbWarfareIntegration.get();
    public static final YwzjVehicleIntegration YWZJ_VEHICLE = YwzjVehicleIntegration.get();
    public static final SpottingIntegration SPOTTING = SpottingIntegration.get();
    public static final PingWheelIntegration PING_WHEEL = PingWheelIntegration.get();
    public static final CustomNpcsIntegration CUSTOM_NPCS = CustomNpcsIntegration.get();
    public static final ConquestBattlefieldIntegration CONQUEST_BATTLEFIELD = ConquestBattlefieldIntegration.get();
    public static final org.mods.gd656killicon.server.logic.integration.ConquestSpottingIntegration CONQUEST_SPOTTING = org.mods.gd656killicon.server.logic.integration.ConquestSpottingIntegration.get();
    public static final org.mods.gd656killicon.server.logic.integration.ImmersiveAircraftIntegration IMMERSIVE_AIRCRAFT = org.mods.gd656killicon.server.logic.integration.ImmersiveAircraftIntegration.get();
    public static final org.mods.gd656killicon.server.logic.honor.HonorEngine HONOR = new org.mods.gd656killicon.server.logic.honor.HonorEngine();

    public static MinecraftServer getServer() {
        return ServerBridge.loader().getCurrentServer();
    }
}
