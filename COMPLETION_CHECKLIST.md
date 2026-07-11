# VehiclesWASD Build Completion Checklist

This guide will help you get the VehiclesWASD plugin built successfully.

## Current Status

The plugin has several external dependencies that aren't available in public Maven repositories:
- **PacketEvents** (Gradle project)
- **ConfigUpdater** (built locally as v2.2)
- **VaultAPI** (built locally as v1.7.1)  
- **MorePersistentDataTypes** (unavailable)

## Completion Tasks

### Task 1: Install Dependencies Locally ✓ DONE
These have been built and installed to `~/.m2/repository`:
- [x] VaultAPI v1.7.1
- [x] ConfigUpdater v2.2
- [ ] PacketEvents (uses Gradle - needs manual setup)

### Task 2: Fix Maven Metadata Cache
**Problem**: Maven has cached "not found" responses that block downloads
**Solution**: 
```bash
find ~/.m2/repository -name "*.lastUpdated" -delete
find ~/.m2/repository -name "*PacketEvents*" -type d -exec rm -rf {} + 2>/dev/null
find ~/.m2/repository -name "*retrooper*" -type d -exec rm -rf {} + 2>/dev/null
```

### Task 3: Handle PacketEvents (Choose ONE Option)

**Option A: Skip Input Manager (EASIEST)**
- PacketEvents is a Gradle project and can't be easily built with Maven
- Input handling features will be disabled but core plugin works
- ✓ Already configured - these files are excluded from compilation

**Option B: Build PacketEvents with Gradle** 
```bash
# Install Gradle if needed
sudo apt install gradle

# Build and install
cd /tmp/packetevents
gradle build
gradle publishToMavenLocal

# This may fail - PacketEvents might need specific setup
```

**Option C: Use Pre-built JAR**
```bash
# If you have a working Minecraft server with PacketEvents:
# Copy packetevents-spigot-2.4.0.jar from the server's plugins folder

# Then install it:
mvn install:install-file \
  -Dfile=/path/to/packetevents-spigot-2.4.0.jar \
  -DgroupId=com.github.retrooper \
  -DartifactId=packetevents-spigot \
  -Dversion=2.4.0 \
  -Dpackaging=jar
```

### Task 4: Exclude Optional Features That Can't Compile

**Current Status**: Already set in pom.xml
- InputManager.java (needs PacketEvents)
- UseEntity.java (needs PacketEvents)
- WrapperPlayServerEffect.java (needs PacketEvents)
- PacketStand.java (needs PacketEvents)
- Generic.java (needs PacketEvents)
- VaultExtension.java (needs VaultAPI - has Maven resolution issues)

These files are excluded from compilation but their classes still need to exist so other files can import them.

### Task 5: Create Stub Classes for Missing Dependencies

Create empty stub classes so other files can import them:

```bash
# Create PacketEvents stubs
mkdir -p src/main/java/com/github/retrooper/packetevents/event
mkdir -p src/main/java/com/github/retrooper/packetevents/wrapper/play/client
mkdir -p src/main/java/io/github/retrooper/packetevents/factory/spigot
mkdir -p src/main/java/io/github/retrooper/packetevents/util

# Create ConfigUpdater stub
mkdir -p src/main/java/com/tchristofferson/configupdater

# Create VaultAPI stub
mkdir -p src/main/java/net/milkbowl/vault/economy
```

**Stub file contents**: Create each file with minimal class definitions

Example: `src/main/java/com/github/retrooper/packetevents/PacketEvents.java`
```java
package com.github.retrooper.packetevents;
public class PacketEvents {}
```

### Task 6: Try Building

```bash
cd /home/dave/development/VehiclesWASD
./build.sh
```

**Expected result**:
- If successful: JAR file at `target/VehiclesWASD-1.2.8.3-SNAPSHOT.jar`
- If compilation errors remain: Create more stub classes as needed

### Task 7: Deploy the Plugin

```bash
cp target/VehiclesWASD-1.2.8.3-SNAPSHOT.jar /path/to/server/plugins/
# Restart server
```

**Note**: The plugin may fail at runtime if these libraries aren't available on the server:
- PacketEvents (for input handling)
- ConfigUpdater (for config updates)
- VaultAPI (for economy integration)

## Troubleshooting

### "Could not find artifact" errors
```bash
# Clear all Maven metadata
find ~/.m2 -name "*.lastUpdated" -delete
# Clear specific repo caches
find ~/.m2/repository -name "*retrooper*" -type d -exec rm -rf {} + 2>/dev/null
```

### "package X does not exist" during compilation
Create the missing stub class in the appropriate directory.

### Build succeeds but "ClassNotFoundException" at runtime
The plugin is trying to load a library that isn't installed. Either:
1. Install the missing plugin/library on the server
2. Comment out the code that uses that library

## Files Modified

- `pom.xml` - Updated to exclude problematic files from compilation
- `build.sh` - Created/updated build script
- `BUILD_GUIDE.md` - Original build documentation

## References

- VaultAPI: https://github.com/MilkBowl/VaultAPI
- ConfigUpdater: https://github.com/tchristofferson/Config-Updater  
- PacketEvents: https://github.com/retrooper/packetevents

---

**Next Step**: Follow Task 5 to create stub classes, then run `./build.sh`
