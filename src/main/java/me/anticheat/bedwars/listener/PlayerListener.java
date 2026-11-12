package me.anticheat.bedwars.listener;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.manager.PunishmentManager;
import me.anticheat.bedwars.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {

    private final BedwarsACPlugin plugin;

    public PlayerListener(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PunishmentManager.ActiveBan activeBan = plugin.getPunishmentManager()
                .getActiveBan(event.getName(), event.getUniqueId());
        if (activeBan != null) {
            String durationStr;
            if (activeBan.getExpiresAt() < 0L) {
                durationStr = "Permanent";
            } else {
                long remaining = Math.max(1000L, activeBan.getExpiresAt() - System.currentTimeMillis());
                durationStr = TimeUtil.formatDuration(remaining);
            }
            String screen = plugin.getPunishmentManager()
                    .buildScreenMessage(activeBan.getReason(), durationStr, activeBan.getBanId());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, screen);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        plugin.getPacketInterceptor().inject(player, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPacketInterceptor().uninject(player);
        plugin.getPlayerDataManager().remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()
                && from.getYaw() == to.getYaw() && from.getPitch() == to.getPitch()) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        plugin.getCheckManager().handleMovement(player, data, from, to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            Vector vel = event.getVelocity();
            data.recordVelocity(vel.getX(), vel.getY(), vel.getZ());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        PlayerData data = plugin.getPlayerDataManager().get(attacker);
        if (data != null) {
            plugin.getCheckManager().handleCombatHit(attacker, data, event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            plugin.getCheckManager().handleBlockPlace(player, data, event.getBlockPlaced(), event.getBlockAgainst());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            plugin.getCheckManager().handleBlockBreak(player, data, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && (event.getItem().getType().isEdible() || event.getItem().getType().name().contains("POTION"))) {
                data.setItemUseStartMillis(System.currentTimeMillis());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            plugin.getCheckManager().handleItemConsume(player, data, System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.setInventoryOpen(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.setInventoryOpen(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) {
            return;
        }
        data.setInventoryOpen(true);
        if (event.getInventory() != null && (event.getInventory().getType() == InventoryType.CHEST
                || event.getInventory().getType() == InventoryType.ENDER_CHEST)) {
            plugin.getCheckManager().handleWindowClickPacket(player, data, event.getRawSlot(), System.currentTimeMillis());
        }
    }
}
