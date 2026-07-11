#!/bin/bash

# Build script for VehiclesWASD Minecraft Plugin

set -e

echo "================================"
echo "VehiclesWASD Plugin Build"
echo "================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

echo "Note: This plugin has some dependencies that are not in public Maven repos:"
echo "  - ConfigUpdater (com.tchristofferson)"
echo "  - PacketEvents (com.github.retrooper)"
echo "  - VaultAPI (net.milkbowl)"
echo ""
echo "To build with all features, you may need to:"
echo "1. Build these dependencies locally and install to ~/.m2/repository"
echo "2. Or use a Minecraft server with these plugins available"
echo ""
echo "Attempting to build without optional integrations..."
echo ""

echo "Cleaning previous builds..."
mvn clean

echo ""
echo "Clearing Maven snapshot cache..."
rm -rf ~/.m2/repository/com/tchristofferson/ 2>/dev/null || true
rm -rf ~/.m2/repository/com/github/retrooper/ 2>/dev/null || true
rm -rf ~/.m2/repository/net/milkbowl/ 2>/dev/null || true

echo ""
echo "Compiling and packaging..."
mvn package -q

echo ""
echo "================================"
echo "Build Complete!"
echo "================================"
echo ""
JAR_FILE=$(find target -name '*.jar' -type f 2>/dev/null | head -1)
if [ -n "$JAR_FILE" ]; then
    echo "JAR file location:"
    echo "  $JAR_FILE"
    echo ""
    echo "File size: $(du -h "$JAR_FILE" | cut -f1)"
else
    echo "ERROR: JAR file was not created!"
    echo "Check the Maven output above for compilation errors."
    exit 1
fi

