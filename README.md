# VehiclesWASD

**Vehicles on a whole different level.**

A Paper/Spigot plugin that adds driveable vehicles controlled with **WASD** (and look direction). Vehicles are built from armor-stand models, support fuel, storage, customization, shops, and optional economy hooks.

**Current version:** `1.3.1.0`  
**Original author:** [Matsubara](https://github.com/aematsubara)  
**This fork:** [dpembo/VehiclesWASD](https://github.com/dpembo/VehiclesWASD)

---

## Features

- **WASD driving** via PacketEvents (smooth client input)
- Multiple vehicle types (land, water, air)
- Fuel system (optional per vehicle)
- Storage inventories
- Custom models (YAML-defined armor stands)
- Shop + customization menus
- Economy support (Vault / PlayerPoints)
- WorldGuard integration (optional)
- Plane weapons / UFO abduction (configurable)
- Action-bar speed & fuel display
- Crafting recipes per vehicle

---

## Vehicle types

| Type        | Class       | Notes                                      |
|-------------|-------------|--------------------------------------------|
| Bike        | Generic     | Land                                       |
| Boat        | Water       | Place on water                             |
| Cybercar    | Generic     | Land, lights                               |
| Helicopter  | UpAndDown   | Vertical flight (space / shift style)      |
| Jetski      | Water       | Place on water                             |
| Kart        | Generic     | Land                                       |
| Plane       | Generic     | Runway takeoff when speed > lifting-speed  |
| Fastplane   | Generic     | Faster plane variant (this fork)           |
| Quad        | Generic     | Land                                       |
| Tank        | Generic     | Land, weapon                               |
| Tractor     | Generic     | Farm modes (path, plant, harvest, etc.)    |
| UFO         | UpAndDown   | Hover + optional abduction                 |

### Plane / Fastplane controls

1. Hold **W** to accelerate on the ground  
2. When speed exceeds **`lifting-speed`**, look **up** while holding W to climb  
3. Look down to descend; A/D to turn  

`lifting-speed` must be **lower** than `max-speed` in config.

---

## Requirements

| Dependency     | Required | Notes                                      |
|----------------|----------|--------------------------------------------|
| **Paper/Spigot** | Yes    | 1.17+ (this fork targets modern Paper, including 1.21.x / 26.x builds) |
| **PacketEvents** | Yes   | Hard dependency (`plugin.yml`)             |
| WorldGuard     | Soft     | Region protection                          |
| Vault          | Soft     | Economy                                    |
| PlayerPoints   | Soft     | Economy alternative                        |
| Essentials     | Soft     | Soft depend                                |
| CMI / CMIEInjector | Soft | Soft depend                            |

---

## Installation

1. Install [PacketEvents](https://modrinth.com/plugin/packetevents) on the server  
2. Drop `VehiclesWASD-*.jar` into `plugins/`  
3. Restart the server (do not only `/reload`)  
4. Edit `plugins/VehiclesWASD/config.yml` as needed  
5. Restart again (or use the plugin’s reload if available)

Models live under `plugins/VehiclesWASD/models/`. Defaults are copied from the jar on first run.

---

## Commands

Main command: **`/vehicleswasd`** (aliases: **`/vwasd`**, **`/vw`**)

| Usage | Description |
|-------|-------------|
| `/vwasd give <type> [player]` | Give a vehicle item |
| `/vwasd shop` | Open the vehicle shop (if enabled) |
| `/vwasd reload` | Reload config (if implemented in your build) |
| `/vwasd help` | Help |

Exact subcommands depend on permissions and your config. Vehicle type names use the config path form, e.g. `plane`, `fastplane`, `cybercar`.

### Examples

```text
/vwasd give plane
/vwasd give fastplane Steve
/vwasd shop
