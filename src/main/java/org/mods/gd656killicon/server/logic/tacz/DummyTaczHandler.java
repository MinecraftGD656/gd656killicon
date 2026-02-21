package org.mods.gd656killicon.server.logic.tacz;

import java.util.UUID;

public class DummyTaczHandler implements ITaczHandler {
    @Override
    public void init() {}

    @Override
    public void tick() {}

    @Override
    public boolean isHeadshotKill(UUID victimId) { return false; }

    @Override
    public boolean isHeadshotDamage(UUID victimId) { return false; }

    @Override
    public boolean isLastBulletKill(UUID victimId) { return false; }

    @Override
    public boolean isGunKill(UUID victimId) { return false; }
}
