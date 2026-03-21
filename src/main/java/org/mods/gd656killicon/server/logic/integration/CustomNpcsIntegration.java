package org.mods.gd656killicon.server.logic.integration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.server.util.ServerLog;

public class CustomNpcsIntegration {
    private static final CustomNpcsIntegration INSTANCE = new CustomNpcsIntegration();
    private boolean initialized = false;
    private boolean loaded = false;
    private Class<?> npcInterfaceClass;

    private CustomNpcsIntegration() {}

    public static CustomNpcsIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        loaded = ModList.get().isLoaded("customnpcs");
        if (!loaded) {
            return;
        }
        try {
            npcInterfaceClass = Class.forName("noppes.npcs.entity.EntityNPCInterface");
            ServerLog.info("CustomNPCs mod detected.");
        } catch (Exception e) {
            loaded = false;
            npcInterfaceClass = null;
            ServerLog.error("Failed to initialize CustomNPCs integration: %s", e.getMessage());
        }
    }

    public String resolveVictimDisplayName(LivingEntity victim, String fallbackName) {
        if (!loaded || npcInterfaceClass == null || victim == null) {
            return fallbackName;
        }
        if (!npcInterfaceClass.isInstance(victim)) {
            return fallbackName;
        }
        String name = victim.getName().getString();
        if (name == null || name.isBlank()) {
            return fallbackName;
        }
        return name;
    }
}
