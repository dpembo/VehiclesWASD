# Building VehiclesWASD Plugin

This plugin has external dependencies that are not available in public Maven repositories. To build it successfully, you have a few options:

## Option 1: Quick Build (Limited Features)

Build without optional integrations:

```bash
./build.sh
```

This will attempt to build the core plugin without:
- Vault economy integration
- PacketEvents input handling  
- ConfigUpdater configuration support

The build may have compilation errors for these features. You'll need to either:
- Fix the compilation errors by removing/stubbing out the missing feature imports
- Or see Option 2 below

## Option 2: Full Build (Recommended)

You need to install the missing dependencies to your local Maven repository first:

```bash
# Install Vault API
cd /tmp
git clone --depth 1 https://github.com/MilkBowl/VaultAPI.git
cd VaultAPI
mvn clean install -DskipTests

# Install ConfigUpdater
# Note: This repository may need to be obtained differently, as it might not be on GitHub
# Check if you have a local copy or contact the author

# Install PacketEvents (if needed for full support)
cd /tmp
git clone --depth 1 https://github.com/retrooper/packetevents.git
cd packetevents
mvn clean install -DskipTests

# Then build the plugin
cd /path/to/VehiclesWASD
./build.sh
```

## Option 3: Using Pre-built Dependencies

If you have access to a working Minecraft server with these plugins already installed, you can extract the JARs from the server's plugins folder and install them to Maven:

```bash
# For each missing dependency JAR
mvn install:install-file \
  -Dfile=/path/to/VaultAPI-1.7.jar \
  -DgroupId=net.milkbowl \
  -DartifactId=VaultAPI \
  -Dversion=1.7 \
  -Dpackaging=jar

# Then build the plugin
./build.sh
```

## Missing Dependencies

The plugin requires these libraries which aren't in public Maven repos:

1. **VaultAPI** - Economy plugin integration
   - GroupId: `net.milkbowl`  
   - ArtifactId: `VaultAPI`
   - Version: `1.7`
   - Source: https://github.com/MilkBowl/VaultAPI

2. **ConfigUpdater** - Configuration file updates
   - GroupId: `com.tchristofferson`
   - ArtifactId: `ConfigUpdater`
   - Version: `2.0`
   - Note: GitHub repository may need to be located manually

3. **PacketEvents** - Packet event handling for input
   - GroupId: `com.github.retrooper`
   - ArtifactId: `packetevents-spigot`
   - Version: `2.4.0`
   - Source: https://github.com/retrooper/packetevents

## Build Output

Once the build completes successfully, the compiled plugin JAR will be located at:

```
target/VehiclesWASD-1.2.8.3-SNAPSHOT.jar
```

Copy this to your Spigot/Paper server's `plugins/` directory and restart the server.

## Troubleshooting

**Q: Build still fails with "could not find artifact"?**
A: The dependency really isn't installed to your local Maven repository. Run the `mvn install` commands for each missing dependency first.

**Q: Can't find ConfigUpdater on GitHub?**
A: Some projects may have been removed or renamed. Try searching for it or contact the original plugin author.

**Q: Compilation errors for Vault/PacketEvents classes?**
A: Comment out the usage of those features in the source code if you don't need them.
