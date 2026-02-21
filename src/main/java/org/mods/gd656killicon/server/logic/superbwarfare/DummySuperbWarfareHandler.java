package org.mods.gd656killicon.server.logic.superbwarfare;

import java.util.UUID;

public class DummySuperbWarfareHandler implements ISuperbWarfareHandler {
    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public void tick() {
        // Do nothing
    }

    @Override
    public boolean isHeadshotKill(UUID victimId) {
        return false;
    }

    @Override
    public boolean isHeadshotDamage(UUID victimId) {
        return false;
    }

    @Override
    public boolean isGunKill(UUID victimId) {
        return false;
    }
}
