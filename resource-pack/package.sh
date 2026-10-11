#!/usr/bin/env bash
# Packages the resource pack into a zip (pack.mcmeta and assets/ at the root of the archive).
# Usage: ./package.sh [output.zip]   (default: build/VehiclesWASD-resource-pack.zip)
set -euo pipefail

cd "$(dirname "$0")"

OUT=${1:-build/VehiclesWASD-resource-pack.zip}
case "$OUT" in /*) ;; *) OUT="$PWD/$OUT" ;; esac

mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"

if command -v zip >/dev/null 2>&1; then
  zip -qr -X "$OUT" pack.mcmeta assets
else
  python3 - "$OUT" <<'E'
import os, sys, zipfile
with zipfile.ZipFile(sys.argv[1], "w", zipfile.ZIP_DEFLATED) as z:
    z.write("pack.mcmeta")
    for root, _, files in os.walk("assets"):
        for f in sorted(files):
            z.write(os.path.join(root, f))
E
fi

echo "Created $OUT"
echo "SHA-1: $(sha1sum "$OUT" | cut -d' ' -f1)"
