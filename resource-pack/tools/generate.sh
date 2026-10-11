#!/usr/bin/env bash
# Regenerates the head models of the resource pack from the textures used by src/main/resources/models/*.yml.
# Run it again whenever a new head texture is added to a model (or to Vehicle.java).
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT=$(pwd)
NS="$ROOT/assets/vehicleswasd"

mkdir -p "$NS/items/head" "$NS/models/item/head" "$NS/textures/item/head"

# Textures that are hardcoded in Vehicle.java (lights).
EXTRA="d9c394fd624e447d125ef3fb54d82a6fa4b0c6188e304d33352d13fbdb0c751b 933799ab30ca9b167c0ee5456c7b12b1247714ff29fb16a491e7d3a636a9aaa3"

HASHES=$( (grep -h "url:" "$ROOT"/../src/main/resources/models/*.yml | awk '{print $2}'; echo "$EXTRA" | tr ' ' '\n') | sort -u)

for hash in $HASHES; do
  [ -s "$NS/textures/item/head/$hash.png" ] || \
    curl -fsS "https://textures.minecraft.net/texture/$hash" -o "$NS/textures/item/head/$hash.png"
  # Old (64x32) skins need different UVs since they're relative to the texture size.
  height=$(python3 -c "import struct,sys;print(struct.unpack('>I',open(sys.argv[1],'rb').read(24)[20:24])[0])" "$NS/textures/item/head/$hash.png")
  template=tools/head_model.template.json
  [ "$height" = "32" ] && template=tools/head_model_64x32.template.json
  sed "s/@HASH@/$hash/g" "$template" > "$NS/models/item/head/$hash.json"
  sed "s/@HASH@/$hash/g" tools/head_item.template.json > "$NS/items/head/$hash.json"
done

echo "Generated $(echo "$HASHES" | wc -l) head models."
