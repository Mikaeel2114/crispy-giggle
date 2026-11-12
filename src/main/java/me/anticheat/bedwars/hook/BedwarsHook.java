package me.anticheat.bedwars.hook;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class BedwarsHook implements Listener {

    private final BedwarsACPlugin plugin;
    private boolean bedwars1058Present = false;
    private boolean bedwarsProxyPresent = false;
    private Object bedwars1058ApiInstance = null;

    public BedwarsHook(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerHooks() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Plugin bw1058 = Bukkit.getPluginManager().getPlugin("BedWars1058");
        if (bw1058 != null && bw1058.isEnabled()) {
            this.bedwars1058Present = true;
            try {
                Class<?> bwApiClass = Class.forName("com.andrei1058.bedwars.api.BedWars");
                this.bedwars1058ApiInstance = Bukkit.getServicesManager().getRegistration(bwApiClass).getProvider();
                plugin.getLogger().info("Hooked into BedWars1058 API successfully.");
            } catch (Throwable t) {
                plugin.getLogger().info("BedWars1058 detected; using reflective arena state inspection.");
            }
        }

        Plugin bwProxy = Bukkit.getPluginManager().getPlugin("BedWarsProxy");
        if (bwProxy != null && bwProxy.isEnabled()) {
            this.bedwarsProxyPresent = true;
            plugin.getLogger().info("Hooked into BedWarsProxy successfully.");
        }
    }

    /**
     * Checks if the player is currently a BedWars spectator or in a non-playing arena state
     * (e.g., Respawning / Spectating in BedWars1058) or standard Bukkit Spectator mode.
     */
    public boolean isSpectatingOrRespawning(Player player) {
        if (player == null) {
            return false;
        }
        if (plugin.getConfigManager().isExemptSpectators()) {
            if (player.getGameMode().name().equals("SPECTATOR") || player.getAllowFlight()) {
                return true;
            }
        }
        if (bedwars1058Present && bedwars1058ApiInstance != null) {
            try {
                Method getArenaUtil = bedwars1058ApiInstance.getClass().getMethod("getArenaUtil");
                Object arenaUtil = getArenaUtil.invoke(bedwars1058ApiInstance);
                if (arenaUtil != null) {
                    Method isSpectating = arenaUtil.getClass().getMethod("isSpectating", Player.class);
                    Object specResult = isSpectating.invoke(arenaUtil, player);
                    if (Boolean.TRUE.equals(specResult)) {
                        return true;
                    }
                    Method getArenaByPlayer = arenaUtil.getClass().getMethod("getArenaByPlayer", Player.class);
                    Object arena = getArenaByPlayer.invoke(arenaUtil, player);
                    if (arena != null) {
                        Method isReSpawning = arena.getClass().getMethod("isReSpawning", Player.class);
                        Object respResult = isReSpawning.invoke(arena, player);
                        if (Boolean.TRUE.equals(respResult)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        PlayerData data = plugin.getPlayerDataManager().get(event.getPlayer());
        if (data != null) {
            data.setRespawnExemptTicks(plugin.getConfigManager().getExemptRespawnTicks());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVoidOrArenaTeleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.getPlayerDataManager().get(event.getPlayer());
        if (data == null) {
            return;
        }
        data.setTeleportTicks(20);
        if (event.getFrom().getY() < 10.0D || event.getTo().distanceSquared(event.getFrom()) > 400.0D) {
            data.setVoidTeleportExemptTicks(plugin.getConfigManager().getExemptVoidTeleportTicks());
            data.setRespawnExemptTicks(plugin.getConfigManager().getExemptRespawnTicks());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosionOrFireballDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            data.setExplosionExemptTicks(plugin.getConfigManager().getExemptExplosionKnockbackTicks());
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Fireball || damager instanceof TNTPrimed) {
                data.setExplosionExemptTicks(plugin.getConfigManager().getExemptExplosionKnockbackTicks());
            }
        }
    }

    public boolean isBedwars1058Present() {
        return bedwars1058Present;
    }

    public boolean isBedwarsProxyPresent() {
        return bedwarsProxyPresent;
    }
}
