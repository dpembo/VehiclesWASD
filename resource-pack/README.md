# VehiclesWASD resource pack

Custom item models for the vehicle heads (wheels, lights, etc.), so clients don't have to download the skins from Mojang
(which makes the parts show up late). Targets Minecraft 26.x (resource pack format 84-88, `items/` definitions).

## Use

1. Merge the `assets/vehicleswasd` folder into your server resource pack (and make sure `pack.mcmeta` covers your version).
2. Set `resource-pack.enabled: true` in the plugin `config.yml` and restart the server.

Each head uses the `item_model` component `vehicleswasd:head/<texture hash>`, which needs Minecraft 1.21.2+.
Players without the pack will see missing-model cubes instead of the heads.

## Packaging

`./package.sh [output.zip]` builds `build/VehiclesWASD-resource-pack.zip` and prints its SHA-1 (useful for `server.properties`'s `resource-pack-sha1`).

## Adding new heads

Add the texture to a model `.yml` (or `Vehicle.java`) and run `tools/generate.sh`, it downloads the skin and generates the
model and item definition from the templates in `tools/`.

The models are a 16px cube (plus the hat layer) so that they match the size of a head worn on an armor stand;
tweak `display.head` in `tools/head_model.template.json` if the size/position needs adjusting.
