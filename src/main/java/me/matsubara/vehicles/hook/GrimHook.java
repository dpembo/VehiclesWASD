package me.matsubara.vehicles.hook;

import me.matsubara.vehicles.VehiclesPlugin;
import me.matsubara.vehicles.vehicle.Vehicle;
import me.matsubara.vehicles.vehicle.VehicleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft-depends on GrimAC.
 * While a player is in a VehiclesWASD vehicle, grants movement-check exemptions
 * via grim.exempt.<check> (not full grim.disabled), so combat/other checks stay active.
 * <p>
 * The set of checks exempted is read from config.yml ({@code grim-exemption.checks.default}
 * plus any additions in {@code grim-exemption.checks.per-vehicle.<type>}), so e.g. air
 * vehicles can exempt the fly/air-liquid checks without loosening them for cars/boats/tanks
 * that never need it.
 */
public final class GrimHook implements Listener {

    private final VehiclesPlugin plugin;

    /** Players we currently have exempted. */
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    /** Cache of resolved (default + per-type) check lists, cleared on {@link #reload()}. */
    private final Map<VehicleType, List<String>> checksByType = new EnumMap<>(VehicleType.class);

    /**
     * Fallback checks used only if grim-exemption.checks.default is missing/empty in config.yml.
     * Adjust after testing with /grim alerts if your build uses different check ids.
     */
    private static final List<String> FALLBACK_DEFAULT_CHECKS = List.of(
            "simulation", "prediction", "offsethandler",
            "timer", "timerlimit",
            "groundspoof", "nofall",
            "speed", "vehicle", "vehiclea", "vehicleb", "vehiclec", "vehicled", "vehiclee", "vehiclef"
    );

    public GrimHook(VehiclesPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("GrimAC") == null) {
            plugin.getLogger().info("GrimAC not found — vehicle movement exemptions disabled.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Hooked into GrimAC (movement checks exempt while in vehicle).");
    }

    /** Call this from your config-reload command so edits to grim-exemption.checks take effect live. */
    public void reload() {
        checksByType.clear();
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("grim-exemption.enabled", false);
    }

    private List<String> getChecksFor(VehicleType type) {
        return checksByType.computeIfAbsent(type, this::resolveChecksFor);
    }

    private List<String> resolveChecksFor(VehicleType type) {
        List<String> defaults = plugin.getConfig().getStringList("grim-exemption.checks.default");
        if (defaults.isEmpty()) defaults = FALLBACK_DEFAULT_CHECKS;

        List<String> extra = plugin.getConfig().getStringList("grim-exemption.checks.per-vehicle." + type.toPath());
        if (extra.isEmpty()) return defaults;

        List<String> merged = new java.util.ArrayList<>(defaults);
        for (String check : extra) {
            if (!merged.contains(check)) merged.add(check);
        }
        return merged;
    }

    public void exempt(Player player, Vehicle vehicle) {
        if (player == null || !player.isOnline() || vehicle == null) return;
        if (!isEnabled()) return;
        if (attachments.containsKey(player.getUniqueId())) return;

        List<String> checks = getChecksFor(vehicle.getType());
        if (checks.isEmpty()) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String check : checks) {
            attachment.setPermission("grim.exempt." + check, true);
        }
        attachments.put(player.getUniqueId(), attachment);
        tryUpdateGrimPermissions(player);
    }

    public void unexempt(Player player) {
        if (player == null) return;
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
                // already removed
            }
        }
        if (player.isOnline()) {
            tryUpdateGrimPermissions(player);
        }
    }

    private void tryUpdateGrimPermissions(Player player) {
        try {
            Class<?> provider = Class.forName("ac.grim.grimac.api.GrimAPIProvider");
            Object api = provider.getMethod("get").invoke(null);
            Object user = api.getClass()
                    .getMethod("getGrimUser", UUID.class)
                    .invoke(api, player.getUniqueId());
            if (user != null) {
                user.getClass().getMethod("updatePermissions").invoke(user);
            }
        } catch (Throwable ignored) {
            // GrimAPI not on classpath or different version — attachment still applies
        }
    }

    /**
     * Resolve a VehiclesWASD vehicle from a mount entity (chair / velocity stand).
     */
    private Vehicle resolveVehicle(Entity mount, Entity rider) {
        if (rider != null) {
            Vehicle byRider = plugin.getVehicleManager().getVehicleByEntity(rider, true);
            if (byRider != null) return byRider;
        }

        if (mount instanceof ArmorStand stand) {
            Vehicle byStand = plugin.getVehicleManager().getVehicleByVelocityStand(stand);
            if (byStand != null) return byStand;

            String modelId = stand.getPersistentDataContainer()
                    .get(plugin.getVehicleModelIdKey(), PersistentDataType.STRING);
            if (modelId != null) {
                try {
                    return plugin.getVehicleManager().getVehicleByModelId(UUID.fromString(modelId));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Chair armor stands
            for (Vehicle vehicle : plugin.getVehicleManager().getVehicles()) {
                if (vehicle.getChairs() == null) continue;
                for (var pair : vehicle.getChairs()) {
                    if (pair != null && pair.getKey() != null && pair.getKey().equals(stand)) {
                        return vehicle;
                    }
                }
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Vehicle vehicle = resolveVehicle(event.getMount(), player);
        if (vehicle == null) return;
        exempt(player, vehicle);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Vehicle vehicle = resolveVehicle(event.getDismounted(), player);
        if (vehicle == null) return;

        // Short delay so exit velocity does not false movement checks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Still in another of our vehicles?
            if (plugin.getVehicleManager().getVehicleByEntity(player, true) != null) return;
            unexempt(player);
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unexempt(event.getPlayer());
    }
}