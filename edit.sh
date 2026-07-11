#!/usr/bin/env bash
set -euo pipefail

# Run this from the VehiclesWASD project root:
#   cd /home/dave/development/VehiclesWASD
#   bash fix_xseries_supports_v2.sh
#
# This covers every remaining bare (major-implied-1) supports(...) call
# found via: grep -rn "XReflection\.supports([0-9]" src/main/java/

FILES=(
  "src/main/java/me/matsubara/vehicles/model/stand/PacketStand.java"
  "src/main/java/me/matsubara/vehicles/util/BlockUtils.java"
  "src/main/java/me/matsubara/vehicles/util/ItemBuilder.java"
  "src/main/java/me/matsubara/vehicles/vehicle/Vehicle.java"
  "src/main/java/me/matsubara/vehicles/manager/InputManager.java"
  "src/main/java/me/matsubara/vehicles/manager/VehicleManager.java"
)

TS=$(date +%Y%m%d-%H%M%S)

for f in "${FILES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "!! File not found, skipping: $f"
    continue
  fi

  cp "$f" "$f.bak-$TS"

  sed -i \
    -e 's/XReflection\.supports(20, 6)/XReflection.supports(1, 20, 6)/g' \
    -e 's/XReflection\.supports(20, 2)/XReflection.supports(1, 20, 2)/g' \
    -e 's/XReflection\.supports(20, 1)/XReflection.supports(1, 20, 1)/g' \
    -e 's/XReflection\.supports(19, 3)/XReflection.supports(1, 19, 3)/g' \
    -e 's/XReflection\.supports(21, 2)/XReflection.supports(1, 21, 2)/g' \
    -e 's/XReflection\.supports(18, 1)/XReflection.supports(1, 18, 1)/g' \
    -e 's/XReflection\.supports(18, 2)/XReflection.supports(1, 18, 2)/g' \
    -e 's/XReflection\.supports(1,19, 3)/XReflection.supports(1, 19, 3)/g' \
    "$f"

  echo "== Diff for $f =="
  diff -u "$f.bak-$TS" "$f" || true
  echo
done

echo "Done. Backups saved as *.bak-$TS alongside each file."
echo
echo "Now run this to confirm nothing bare is left:"
echo '  grep -rn "XReflection\.supports([0-9]" src/main/java/ | grep -v ".bak"'
echo "Then rebuild: ./build.sh"
