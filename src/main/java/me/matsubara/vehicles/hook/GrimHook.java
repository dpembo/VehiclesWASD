package me.matsubara.vehicles.hook;

import me.matsubara.vehicles.VehiclesPlugin;
import me.matsubara.vehicles.vehicle.Vehicle;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft-depends on GrimAC.
 * While a player is in a VehiclesWASD vehicle, grants movement-check exemptions
 * via grim.exempt.<check> (not full grim.disabled), so combat/other checks stay active.
 */
public final class GrimHook implements Listener {

    private final VehiclesPlugin plugin;

    /** Players we currently have exempted. */
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    /**
     * Grim check config names (lowercase). Adjust after testing with /grim alerts
     * if your build uses different ids.
     */
    private static final String[] MOVEMENT_CHECKS = {
            // Prediction / simulation
            "simulation",
            "prediction",
            "offsethandler",
            // Timer
            "timer",
            "timerlimit",
            // Ground / air
            "groundspoof",
            "nofall",
            "fly",
            "airliquid",
            // Speed / vehicle-related
            "speed",
            "vehicle",
            "vehiclea",
            "vehicleb",
            "vehiclec",
            "vehicled",
            "vehiclee",
            "vehiclef",
            // Common extras that fire on custom movement
            "knockback",
            "explosion",
            "negativetimer",
            "badpackets"
    };

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

    public void exempt(Player player) {
        if (player == null || !player.isOnline()) return;
        if (attachments.containsKey(player.getUniqueId())) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String check : MOVEMENT_CHECKS) {
            attachment.setPermission("grim.exempt." + check, true);
            // Avoid rubber-banding on those checks if they still fire under another name
            attachment.setPermission("grim.nosetback." + check, true);
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
        exempt(player);
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