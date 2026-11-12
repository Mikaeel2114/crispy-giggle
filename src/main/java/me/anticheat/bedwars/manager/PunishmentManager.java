package me.anticheat.bedwars.manager;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.config.ConfigManager;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PunishmentManager {

    private final BedwarsACPlugin plugin;
    private Connection sqliteConnection;
    private File flatFile;
    private YamlConfiguration flatConfig;

    public PunishmentManager(BedwarsACPlugin plugin) {
        this.plugin = plugin;
        initStorage();
    }

    private void initStorage() {
        ConfigManager cfg = plugin.getConfigManager();
        this.flatFile = new File(plugin.getDataFolder(), cfg.getFlatFileName());
        if (!this.flatFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                this.flatFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        this.flatConfig = YamlConfiguration.loadConfiguration(this.flatFile);

        if (cfg.isUseSqlite()) {
            try {
                Class.forName("org.sqlite.JDBC");
                File dbFile = new File(plugin.getDataFolder(), cfg.getSqliteFileName());
                this.sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                Statement stmt = this.sqliteConnection.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ac_offenses ("
                        + "uuid TEXT PRIMARY KEY, "
                        + "player_name TEXT, "
                        + "offense_count INTEGER, "
                        + "last_offense_time INTEGER)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ac_bans ("
                        + "ban_id TEXT PRIMARY KEY, "
                        + "uuid TEXT, "
                        + "player_name TEXT, "
                        + "reason TEXT, "
                        + "created_at INTEGER, "
                        + "expires_at INTEGER, "
                        + "active INTEGER)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ac_logs ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "uuid TEXT, "
                        + "player_name TEXT, "
                        + "check_name TEXT, "
                        + "vl INTEGER, "
                        + "max_vl INTEGER, "
                        + "ping INTEGER, "
                        + "tps REAL, "
                        + "details TEXT, "
                        + "timestamp INTEGER)");
                stmt.close();
            } catch (Throwable t) {
                plugin.getLogger().warning("SQLite unavailable, falling back to YAML flat-file storage: " + t.getMessage());
                this.sqliteConnection = null;
            }
        }
    }

    public void punish(final Player player, final PlayerData data, final Check check) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    punishSync(player, data, check);
                }
            });
        } else {
            punishSync(player, data, check);
        }
    }

    private void punishSync(Player player, PlayerData data, Check check) {
        if (player == null || !player.isOnline()) {
            return;
        }
        data.resetVl();

        ConfigManager cfg = plugin.getConfigManager();
        int offenseCount = incrementAndGetOffenseCount(player.getUniqueId(), player.getName());
        ConfigManager.OffenseStep step = cfg.getOffenseStep(offenseCount);

        String type = step.getType();
        String durationStr = step.getDuration();
        long durationMillis = TimeUtil.parseDurationMillis(durationStr);
        String reason = check.getReason();
        String banId = generateBanId();

        if (cfg.isBroadcastEnabled()) {
            String msg = cfg.getBroadcastMessage().replace("%player%", player.getName());
            Bukkit.broadcastMessage(msg);
        }

        if (type.equalsIgnoreCase("KICK")) {
            String kickScreen = buildScreenMessage(reason, "Instant (Kick)", banId);
            player.kickPlayer(kickScreen);
            String cmd = cfg.getKickCommand()
                    .replace("%player%", player.getName())
                    .replace("%reason%", reason)
                    .replace("%id%", banId);
            if (!cmd.trim().isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        } else if (type.equalsIgnoreCase("TEMPBAN")) {
            long expiresAt = System.currentTimeMillis() + Math.max(60000L, durationMillis);
            saveBanRecord(banId, player.getUniqueId(), player.getName(), reason, System.currentTimeMillis(), expiresAt);
            String kickScreen = buildScreenMessage(reason, durationStr, banId);
            player.kickPlayer(kickScreen);
            String cmd = cfg.getTempbanCommand()
                    .replace("%player%", player.getName())
                    .replace("%duration%", durationStr)
                    .replace("%reason%", reason)
                    .replace("%id%", banId);
            if (!cmd.trim().isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        } else {
            saveBanRecord(banId, player.getUniqueId(), player.getName(), reason, System.currentTimeMillis(), -1L);
            String kickScreen = buildScreenMessage(reason, "Permanent", banId);
            player.kickPlayer(kickScreen);
            String cmd = cfg.getBanCommand()
                    .replace("%player%", player.getName())
                    .replace("%reason%", reason)
                    .replace("%id%", banId);
            if (!cmd.trim().isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    public String manualBan(String targetName, UUID targetUuid, String reason) {
        String banId = generateBanId();
        long now = System.currentTimeMillis();
        saveBanRecord(banId, targetUuid, targetName, reason, now, -1L);

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null && online.isOnline()) {
            online.kickPlayer(buildScreenMessage(reason, "Permanent", banId));
        }
        return banId;
    }

    public boolean unbanPlayer(String targetName) {
        boolean removed = false;
        if (sqliteConnection != null) {
            try {
                PreparedStatement ps = sqliteConnection.prepareStatement(
                        "UPDATE ac_bans SET active = 0 WHERE LOWER(player_name) = ? AND active = 1");
                ps.setString(1, targetName.toLowerCase(Locale.ROOT));
                int affected = ps.executeUpdate();
                ps.close();
                if (affected > 0) {
                    removed = true;
                }
            } catch (Exception ignored) {
            }
        }
        synchronized (this) {
            String key = "bans." + targetName.toLowerCase(Locale.ROOT);
            if (flatConfig.contains(key)) {
                flatConfig.set(key + ".active", false);
                saveFlatFile();
                removed = true;
            }
        }
        return removed;
    }

    public ActiveBan getActiveBan(String playerName, UUID uuid) {
        long now = System.currentTimeMillis();
        if (sqliteConnection != null) {
            try {
                PreparedStatement ps = sqliteConnection.prepareStatement(
                        "SELECT ban_id, reason, expires_at FROM ac_bans WHERE (uuid = ? OR LOWER(player_name) = ?) AND active = 1 ORDER BY created_at DESC LIMIT 1");
                ps.setString(1, uuid != null ? uuid.toString() : "");
                ps.setString(2, playerName.toLowerCase(Locale.ROOT));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String banId = rs.getString("ban_id");
                    String reason = rs.getString("reason");
                    long expiresAt = rs.getLong("expires_at");
                    rs.close();
                    ps.close();
                    if (expiresAt > 0L && now > expiresAt) {
                        unbanPlayer(playerName);
                        return null;
                    }
                    return new ActiveBan(banId, reason, expiresAt);
                }
                rs.close();
                ps.close();
            } catch (Exception ignored) {
            }
        }
        synchronized (this) {
            String base = "bans." + playerName.toLowerCase(Locale.ROOT);
            if (flatConfig.getBoolean(base + ".active", false)) {
                long expiresAt = flatConfig.getLong(base + ".expires-at", -1L);
                if (expiresAt > 0L && now > expiresAt) {
                    flatConfig.set(base + ".active", false);
                    saveFlatFile();
                    return null;
                }
                String banId = flatConfig.getString(base + ".ban-id", "AC-0000");
                String reason = flatConfig.getString(base + ".reason", "Unfair Advantage");
                return new ActiveBan(banId, reason, expiresAt);
            }
        }
        return null;
    }

    public String buildScreenMessage(String reason, String durationDisplay, String banId) {
        List<String> lines = plugin.getConfigManager().getScreenMessage();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String formatted = lines.get(i)
                    .replace("%reason%", reason)
                    .replace("%duration%", durationDisplay)
                    .replace("%id%", banId);
            sb.append(formatted);
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private synchronized int incrementAndGetOffenseCount(UUID uuid, String playerName) {
        long now = System.currentTimeMillis();
        long resetWindow = plugin.getConfigManager().getResetOffensesAfterMillis();
        int count = 0;
        long lastTime = 0L;

        if (sqliteConnection != null) {
            try {
                PreparedStatement select = sqliteConnection.prepareStatement(
                        "SELECT offense_count, last_offense_time FROM ac_offenses WHERE uuid = ?");
                select.setString(1, uuid.toString());
                ResultSet rs = select.executeQuery();
                if (rs.next()) {
                    count = rs.getInt("offense_count");
                    lastTime = rs.getLong("last_offense_time");
                }
                rs.close();
                select.close();

                if (lastTime > 0L && resetWindow > 0L && (now - lastTime) > resetWindow) {
                    count = 0;
                }
                count++;

                PreparedStatement upsert = sqliteConnection.prepareStatement(
                        "INSERT OR REPLACE INTO ac_offenses (uuid, player_name, offense_count, last_offense_time) VALUES (?, ?, ?, ?)");
                upsert.setString(1, uuid.toString());
                upsert.setString(2, playerName);
                upsert.setInt(3, count);
                upsert.setLong(4, now);
                upsert.executeUpdate();
                upsert.close();
                return count;
            } catch (Exception ignored) {
            }
        }

        String path = "offenses." + uuid.toString();
        count = flatConfig.getInt(path + ".count", 0);
        lastTime = flatConfig.getLong(path + ".last-time", 0L);
        if (lastTime > 0L && resetWindow > 0L && (now - lastTime) > resetWindow) {
            count = 0;
        }
        count++;
        flatConfig.set(path + ".name", playerName);
        flatConfig.set(path + ".count", count);
        flatConfig.set(path + ".last-time", now);
        saveFlatFile();
        return count;
    }

    private synchronized void saveBanRecord(String banId, UUID uuid, String playerName, String reason, long createdAt, long expiresAt) {
        if (sqliteConnection != null) {
            try {
                PreparedStatement ps = sqliteConnection.prepareStatement(
                        "INSERT INTO ac_bans (ban_id, uuid, player_name, reason, created_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, 1)");
                ps.setString(1, banId);
                ps.setString(2, uuid != null ? uuid.toString() : "");
                ps.setString(3, playerName);
                ps.setString(4, reason);
                ps.setLong(5, createdAt);
                ps.setLong(6, expiresAt);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ignored) {
            }
        }
        String base = "bans." + playerName.toLowerCase(Locale.ROOT);
        flatConfig.set(base + ".ban-id", banId);
        flatConfig.set(base + ".uuid", uuid != null ? uuid.toString() : "");
        flatConfig.set(base + ".reason", reason);
        flatConfig.set(base + ".created-at", createdAt);
        flatConfig.set(base + ".expires-at", expiresAt);
        flatConfig.set(base + ".active", true);
        saveFlatFile();
    }

    public void logViolationAsync(final UUID uuid, final String playerName, final String checkName,
                                  final int vl, final int maxVl, final int ping, final double tps, final String details) {
        final long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                synchronized (PunishmentManager.this) {
                    if (sqliteConnection != null) {
                        try {
                            PreparedStatement ps = sqliteConnection.prepareStatement(
                                    "INSERT INTO ac_logs (uuid, player_name, check_name, vl, max_vl, ping, tps, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                            ps.setString(1, uuid.toString());
                            ps.setString(2, playerName);
                            ps.setString(3, checkName);
                            ps.setInt(4, vl);
                            ps.setInt(5, maxVl);
                            ps.setInt(6, ping);
                            ps.setDouble(7, tps);
                            ps.setString(8, details != null ? details : "");
                            ps.setLong(9, now);
                            ps.executeUpdate();
                            ps.close();
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                    String key = "logs." + playerName.toLowerCase(Locale.ROOT);
                    List<String> existing = flatConfig.getStringList(key);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String line = "[" + sdf.format(new Date(now)) + "] " + checkName + " (VL " + vl + "/" + maxVl
                            + ") | Ping " + ping + "ms | TPS " + String.format("%.2f", tps)
                            + (details != null && !details.isEmpty() ? " | " + details : "");
                    existing.add(line);
                    if (existing.size() > 100) {
                        existing.remove(0);
                    }
                    flatConfig.set(key, existing);
                    saveFlatFile();
                }
            }
        });
    }

    public synchronized List<String> getRecentLogs(String playerName, int limit) {
        List<String> results = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
        if (sqliteConnection != null) {
            try {
                PreparedStatement ps = sqliteConnection.prepareStatement(
                        "SELECT check_name, vl, max_vl, ping, tps, details, timestamp FROM ac_logs WHERE LOWER(player_name) = ? ORDER BY timestamp DESC LIMIT ?");
                ps.setString(1, playerName.toLowerCase(Locale.ROOT));
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String check = rs.getString("check_name");
                    int vl = rs.getInt("vl");
                    int maxVl = rs.getInt("max_vl");
                    int ping = rs.getInt("ping");
                    double tps = rs.getDouble("tps");
                    String details = rs.getString("details");
                    long ts = rs.getLong("timestamp");
                    results.add("&7[" + sdf.format(new Date(ts)) + "] &c" + check + " &7(VL &f" + vl + "/" + maxVl
                            + "&7) &8| &7Ping &f" + ping + " &7TPS &f" + String.format("%.1f", tps)
                            + (details != null && !details.isEmpty() ? " &8(&7" + details + "&8)" : ""));
                }
                rs.close();
                ps.close();
                if (!results.isEmpty()) {
                    return results;
                }
            } catch (Exception ignored) {
            }
        }
        List<String> raw = flatConfig.getStringList("logs." + playerName.toLowerCase(Locale.ROOT));
        for (int i = raw.size() - 1; i >= 0 && results.size() < limit; i--) {
            results.add("&7" + raw.get(i));
        }
        return results;
    }

    private String generateBanId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("#");
        long seed = System.nanoTime();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.abs(seed + i * 31L) % chars.length());
            sb.append(chars.charAt(idx));
            seed /= 7L;
        }
        return sb.toString();
    }

    private synchronized void saveFlatFile() {
        try {
            flatConfig.save(flatFile);
        } catch (IOException ignored) {
        }
    }

    public synchronized void close() {
        if (sqliteConnection != null) {
            try {
                sqliteConnection.close();
            } catch (Exception ignored) {
            }
        }
        saveFlatFile();
    }

    public static class ActiveBan {
        private final String banId;
        private final String reason;
        private final long expiresAt;

        public ActiveBan(String banId, String reason, long expiresAt) {
            this.banId = banId;
            this.reason = reason;
            this.expiresAt = expiresAt;
        }

        public String getBanId() {
            return banId;
        }

        public String getReason() {
            return reason;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
