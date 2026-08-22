package me.matsubara.vehicles.hook;

import me.matsubara.vehicles.VehiclesPlugin;
import me.matsubara.vehicles.files.Config;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.ChunkyBorderProvider;

/**
 * We deliberately DON'T hook into ChunkyBorder's own teleport/wrap logic.
 * <p>
 * ChunkyBorder only knows about {@code player.getVehicle()}, which (when riding) is just the
 * "chair" ArmorStand the player is a native Bukkit passenger of. It teleports that stand + the
 * player together, but has no idea that the chair is actually parented to our own
 * {@code velocityStand}, or that the rest of the model is made of packet-only entities.
 * <p>
 * On the very next tick, our own movement code re-syncs the chair (and everything else) back to
 * a position relative to {@code velocityStand}, which never moved — undoing ChunkyBorder's
 * teleport and leaving the visible model behind. That's the "doesn't function well" behaviour.
 * <p>
 * Rather than trying to co-opt ChunkyBorder's wrap (which would mean also snapping every packet
 * stand + the velocityStand across the map in lockstep, on a plugin we don't control the release
 * cadence of), we simply stop the vehicle before it ever reaches ChunkyBorder's real border, the
 * same way {@link me.matsubara.vehicles.vehicle.Vehicle#notAllowedHere(Location)} already stops
 * it at the vanilla {@link World#getWorldBorder()}.
 */
public class ChunkyBorderExtension implements AVExtension<ChunkyBorderExtension> {

    // Resolved lazily. ChunkyBorder's plugin.yml declares "load: POSTWORLD", while ours is
    // "load: STARTUP" - softdepend only orders plugins within the same load phase, so
    // ChunkyBorderProvider genuinely isn't registered yet when our onEnable() runs. Grabbing it
    // here instead (on first real use, well after server startup) sidesteps that entirely.
    private ChunkyBorder chunkyBorder;
    private VehiclesPlugin plugin;
    private boolean lookupAttempted;

    @Override
    public ChunkyBorderExtension init(@NotNull VehiclesPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("ChunkyBorder extension loaded.");
        return this;
    }

    private @Nullable ChunkyBorder chunkyBorder() {
        if (chunkyBorder != null){
            //plugin.getLogger().info("Hooked into ChunkyBorder — border stop enforcement active.");
            return chunkyBorder;
        }
        if (lookupAttempted) return null;
        lookupAttempted = true;
        try {
            chunkyBorder = ChunkyBorderProvider.get();
            plugin.getLogger().info("Hooked into ChunkyBorder — border stop enforcement active.");
            return chunkyBorder;
        } catch (IllegalStateException exception) {
            plugin.getLogger().warning("ChunkyBorder not available — border stop enforcement disabled.");
            return null;
        }
    }

    /**
     * @return true if the location is inside the buffer zone of ChunkyBorder's configured border
     * (or completely outside of it), meaning the vehicle should stop here.
     */
    public boolean tooCloseToBorder(@NotNull Location location) {
        ChunkyBorder chunkyBorder = chunkyBorder();
        if (chunkyBorder == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        BorderData border = chunkyBorder.getBorder(world.getName()).orElse(null);
        if (border == null) return false;

        double buffer = Math.max(0.0d, Config.CHUNKY_BORDER_BUFFER.asDouble());

        // Outside of the border entirely -> definitely not allowed.
        if (!border.getBorder().isBounding(location.getX(), location.getZ())) return true;

        // Rectangle/square borders (the only shapes ChunkyBorder currently wraps) have a fixed
        // center + radius, so we can work out the distance to the nearest edge directly instead
        // of needing anything more from ChunkyBorder's Shape interface.
        double distanceX = border.getRadiusX() - Math.abs(location.getX() - border.getCenterX());
        double distanceZ = border.getRadiusZ() - Math.abs(location.getZ() - border.getCenterZ());

        return Math.min(distanceX, distanceZ) <= buffer;
    }
}
