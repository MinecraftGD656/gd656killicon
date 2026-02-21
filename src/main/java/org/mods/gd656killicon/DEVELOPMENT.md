# GD656Killicon Server-Side Implementation Guide

## Overview

This module manages server-side event handling for kill confirmations, including special kill types like headshots (via TACZ integration) and explosions. It communicates with the client via `KillIconPacket`.

## Core Components

### 1. `DeathEventHandler.java`
- **Purpose**: Listens to vanilla `LivingDeathEvent` to detect entity kills.
- **Priority**: `EventPriority.LOWEST` to ensure it runs after other mods (like TACZ) have processed the event.
- **Logic**:
  - Checks if the attacker is a `ServerPlayer`.
  - Determines `killType` (0=Normal, 1=Headshot, 2=Explosion).
  - Queries `TaczEventHandler` to see if the kill was already handled as a headshot.
  - Sends `KillIconPacket` to the client.

### 2. `TaczEventHandler.java`
- **Purpose**: Provides optional integration with the TACZ (Timeless and Classics Zero) gun mod.
- **Mechanism**: Uses Java Reflection to avoid hard dependencies. If TACZ classes are found, it registers a listener.
- **Event Handling**:
  - Listens for `EntityKillByGunEvent`.
  - If a headshot is detected:
    1. **Direct Send**: Immediately sends a `KillIconPacket` (Type 1) to the client to avoid race conditions with `LivingDeathEvent`.
    2. **Record**: Adds the victim's UUID to `headshotVictims` set so `DeathEventHandler` knows to skip this kill.

### 3. `KillIconPacket.java`
- **Fields**:
  - `category`: "kill_icon"
  - `name`: "scrolling"
  - `killType`: Integer representing the kill type.
- **Handling**: Triggers the client-side HUD renderer with the specific kill type.

## Kill Types
| Type ID | Description | Condition |
|---------|-------------|-----------|
| 0 | Normal | Default kill (melee, projectile, etc.) |
| 1 | Headshot | Triggered by TACZ gun headshot |
| 2 | Explosion | Triggered by `DamageTypes.EXPLOSION`, `PLAYER_EXPLOSION`, or msgId containing "explosion" |
| 3 | Crit | Triggered by Critical Hit |

## Maintenance Notes
- **TACZ Updates**: If TACZ changes its API package path, update the reflection strings in `TaczEventHandler.init()`.
- **Race Conditions**: The direct packet send in `TaczEventHandler` is crucial. Do not rely solely on `DeathEventHandler` querying the state, as event order is not guaranteed across different mod loader versions.
