package me.anticheat.bedwars.config;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final BedwarsACPlugin plugin;

    // Alerts
    private boolean alertsEnabled;
    private String alertsPermission;
    private boolean notifyOps;
    private boolean notifyConsole;
    private String alertSound;
    private int alertCooldownSeconds;
    private String alertMessage;
    private boolean clickTeleport;
    private String discordWebhook;
    private String teleportCommandPrefix;
    private String hoverTextFormat;

    // General
    private double minTps;
    private int maxPingExempt;
    private int vlDecaySeconds;
    private int vlDecayAmount;
    private double bufferDecayMultiplier;
    private String exemptPermission;
    private String adminPermission;
    private boolean useSqlite;
    private String sqliteFileName;
    private String flatFileName;

    // Punishments
    private final Map<Integer, OffenseStep> punishmentLadder = new HashMap<Integer, OffenseStep>();
    private long resetOffensesAfterMillis;
    private boolean broadcastEnabled;
    private String broadcastMessage;
    private String kickCommand;
    private String tempbanCommand;
    private String banCommand;
    private List<String> screenMessage = new ArrayList<String>();

    // Bedwars
    private int exemptRespawnTicks;
    private int exemptExplosionKnockbackTicks;
    private int exemptVoidTeleportTicks;
    private boolean exemptSpectators;

    public ConfigManager(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        registerDynamicDefaults(cfg);

        // Alerts section
        this.alertsEnabled = cfg.getBoolean("alerts.enabled");
        this.alertsPermission = cfg.getString("alerts.permission");
        this.notifyOps = cfg.getBoolean("alerts.notify-ops");
        this.notifyConsole = cfg.getBoolean("alerts.notify-console");
        this.alertSound = cfg.getString("alerts.sound");
        this.alertCooldownSeconds = cfg.getInt("alerts.cooldown-seconds");
        this.alertMessage = color(cfg.getString("alerts.message"));
        this.clickTeleport = cfg.getBoolean("alerts.click-teleport");
        this.discordWebhook = cfg.getString("alerts.discord-webhook");
        this.teleportCommandPrefix = cfg.getString("alerts.teleport-command");
        this.hoverTextFormat = color(cfg.getString("alerts.hover-message"));

        // General section
        this.minTps = cfg.getDouble("general.min-tps");
        this.maxPingExempt = cfg.getInt("general.max-ping-exempt");
        this.vlDecaySeconds = cfg.getInt("general.vl-decay-seconds");
        this.vlDecayAmount = cfg.getInt("general.vl-decay-amount");
        this.bufferDecayMultiplier = cfg.getDouble("general.buffer-decay-multiplier");
        this.exemptPermission = cfg.getString("general.exempt-permission");
        this.adminPermission = cfg.getString("general.admin-permission");
        this.useSqlite = cfg.getBoolean("general.use-sqlite");
        this.sqliteFileName = cfg.getString("general.sqlite-file");
        this.flatFileName = cfg.getString("general.flatfile-name");

        // Punishments section
        this.punishmentLadder.clear();
        ConfigurationSection ladderSec = cfg.getConfigurationSection("punishments.ladder");
        if (ladderSec != null) {
            for (String key : ladderSec.getKeys(false)) {
                int index = parseOffenseIndex(key);
                if (index > 0) {
                    String type = ladderSec.getString(key + ".type");
                    String duration = ladderSec.getString(key + ".duration");
                    punishmentLadder.put(index, new OffenseStep(type, duration));
                }
            }
        }
        this.resetOffensesAfterMillis = TimeUtil.parseDurationMillis(cfg.getString("punishments.reset-offenses-after"));
        this.broadcastEnabled = cfg.getBoolean("punishments.broadcast.enabled");
        this.broadcastMessage = color(cfg.getString("punishments.broadcast.message"));
        this.kickCommand = cfg.getString("punishments.ban-commands.kick");
        this.tempbanCommand = cfg.getString("punishments.ban-commands.tempban");
        this.banCommand = cfg.getString("punishments.ban-commands.ban");

        List<String> rawScreen = cfg.getStringList("punishments.screen-message");
        this.screenMessage = new ArrayList<String>();
        for (String line : rawScreen) {
            this.screenMessage.add(color(line));
        }

        // Bedwars section
        this.exemptRespawnTicks = cfg.getInt("bedwars.exempt-respawn-ticks");
        this.exemptExplosionKnockbackTicks = cfg.getInt("bedwars.exempt-explosion-knockback-ticks");
        this.exemptVoidTeleportTicks = cfg.getInt("bedwars.exempt-void-teleport-ticks");
        this.exemptSpectators = cfg.getBoolean("bedwars.exempt-spectators");
    }

    private void registerDynamicDefaults(FileConfiguration cfg) {
        // Sub-parameters for checks and general settings so nothing is hardcoded in check classes
        cfg.addDefault("alerts.teleport-command", "/tp %player%");
        cfg.addDefault("alerts.hover-message", "&7Click to teleport to &c%player%\n&7Details: &f%details%");
        cfg.addDefault("general.vl-decay-amount", 1);
        cfg.addDefault("general.buffer-decay-multiplier", 0.75);
        cfg.addDefault("general.admin-permission", "anticheat.admin");
        cfg.addDefault("general.use-sqlite", true);
        cfg.addDefault("general.sqlite-file", "anticheat.db");
        cfg.addDefault("general.flatfile-name", "storage.yml");
        cfg.addDefault("bedwars.exempt-void-teleport-ticks", 40);

        // Combat check threshold defaults
        cfg.addDefault("checks.killaura-a.buffer-max", 2.0);
        cfg.addDefault("checks.killaura-a.buffer-decay", 0.25);
        cfg.addDefault("checks.killaura-a.max-targets-per-tick", 1);

        cfg.addDefault("checks.killaura-b.buffer-max", 4.0);
        cfg.addDefault("checks.killaura-b.max-attacks-per-tick", 2);
        cfg.addDefault("checks.killaura-b.buffer-decay", 0.25);
        cfg.addDefault("checks.killaura-b.post-flying-max-ms", 3L);

        cfg.addDefault("checks.killaura-c.yaw-snap-threshold", 100.0);
        cfg.addDefault("checks.killaura-c.min-target-distance", 1.2);
        cfg.addDefault("checks.killaura-c.buffer-max", 2.0);
        cfg.addDefault("checks.killaura-c.buffer-decay", 0.25);

        cfg.addDefault("checks.reach.high-ping-threshold", 150);
        cfg.addDefault("checks.reach.hitbox-expand", 0.13);
        cfg.addDefault("checks.reach.backtrack-window-ms", 100);
        cfg.addDefault("checks.reach.buffer-max", 2.5);
        cfg.addDefault("checks.reach.buffer-decay", 0.35);

        cfg.addDefault("checks.autoclicker-a.window-seconds", 2);
        cfg.addDefault("checks.autoclicker-a.buffer-max", 2.0);
        cfg.addDefault("checks.autoclicker-a.buffer-decay", 0.25);

        cfg.addDefault("checks.autoclicker-b.sample-size", 30);
        cfg.addDefault("checks.autoclicker-b.min-std-dev", 3.5);
        cfg.addDefault("checks.autoclicker-b.max-click-interval-ms", 180L);
        cfg.addDefault("checks.autoclicker-b.buffer-max", 3.0);
        cfg.addDefault("checks.autoclicker-b.buffer-decay", 0.5);

        cfg.addDefault("checks.velocity.required-streak", 3);
        cfg.addDefault("checks.velocity.min-expected-vy", 0.15);
        cfg.addDefault("checks.velocity.buffer-max", 3.0);
        cfg.addDefault("checks.velocity.buffer-decay", 0.5);

        cfg.addDefault("checks.criticals.buffer-max", 4.0);
        cfg.addDefault("checks.criticals.buffer-decay", 0.25);

        cfg.addDefault("checks.noslowdown.enabled", true);
        cfg.addDefault("checks.noslowdown.max-vl", 8);
        cfg.addDefault("checks.noslowdown.punish", true);
        cfg.addDefault("checks.noslowdown.reason", "Unfair Advantage (Combat/NoSlowdown)");
        cfg.addDefault("checks.noslowdown.max-item-use-speed", 0.23);
        cfg.addDefault("checks.noslowdown.keepsprint-min-ratio", 0.96);
        cfg.addDefault("checks.noslowdown.buffer-max", 4.0);
        cfg.addDefault("checks.noslowdown.buffer-decay", 0.5);

        // Movement check threshold defaults
        cfg.addDefault("checks.speed.base-ground-speed", 0.2873);
        cfg.addDefault("checks.speed.base-air-speed", 0.36);
        cfg.addDefault("checks.speed.air-friction", 0.91);
        cfg.addDefault("checks.speed.air-acceleration", 0.026);
        cfg.addDefault("checks.speed.jump-boost-allowance", 0.22);
        cfg.addDefault("checks.speed.speed-potion-multiplier", 0.20);
        cfg.addDefault("checks.speed.exempt-ice-ticks", 40);
        cfg.addDefault("checks.speed.exempt-slime-ticks", 40);
        cfg.addDefault("checks.speed.exempt-stairs-ticks", 20);
        cfg.addDefault("checks.speed.exempt-velocity-ticks", 25);
        cfg.addDefault("checks.speed.buffer-max", 3.0);
        cfg.addDefault("checks.speed.buffer-decay", 0.3);

        cfg.addDefault("checks.fly.gravity-drag", 0.98);
        cfg.addDefault("checks.fly.gravity-drop", 0.08);
        cfg.addDefault("checks.fly.hover-tolerance", 0.015);
        cfg.addDefault("checks.fly.buffer-max", 2.5);
        cfg.addDefault("checks.fly.buffer-decay", 0.25);

        cfg.addDefault("checks.timer.sample-size", 20);
        cfg.addDefault("checks.timer.expected-tick-ms", 50.0);
        cfg.addDefault("checks.timer.max-speed-multiplier", 1.075);
        cfg.addDefault("checks.timer.buffer-max", 4.0);
        cfg.addDefault("checks.timer.buffer-decay", 0.5);

        cfg.addDefault("checks.nofall.min-fall-distance", 3.0);
        cfg.addDefault("checks.nofall.buffer-max", 2.0);
        cfg.addDefault("checks.nofall.buffer-decay", 0.25);

        cfg.addDefault("checks.jesus.enabled", true);
        cfg.addDefault("checks.jesus.max-vl", 6);
        cfg.addDefault("checks.jesus.punish", true);
        cfg.addDefault("checks.jesus.reason", "Unfair Advantage (Movement/Jesus)");
        cfg.addDefault("checks.jesus.max-liquid-speed", 0.26);
        cfg.addDefault("checks.jesus.buffer-max", 3.0);
        cfg.addDefault("checks.jesus.buffer-decay", 0.35);

        cfg.addDefault("checks.step.enabled", true);
        cfg.addDefault("checks.step.max-vl", 6);
        cfg.addDefault("checks.step.punish", true);
        cfg.addDefault("checks.step.reason", "Unfair Advantage (Movement/Step)");
        cfg.addDefault("checks.step.max-step-height", 0.6001);
        cfg.addDefault("checks.step.buffer-max", 1.5);
        cfg.addDefault("checks.step.buffer-decay", 0.25);

        cfg.addDefault("checks.phase.enabled", true);
        cfg.addDefault("checks.phase.max-vl", 6);
        cfg.addDefault("checks.phase.punish", true);
        cfg.addDefault("checks.phase.reason", "Unfair Advantage (Movement/Phase)");
        cfg.addDefault("checks.phase.max-phase-distance", 0.35);
        cfg.addDefault("checks.phase.buffer-max", 2.0);
        cfg.addDefault("checks.phase.buffer-decay", 0.25);

        // World / Bedwars check threshold defaults
        cfg.addDefault("checks.scaffold.min-valid-pitch", -15.0);
        cfg.addDefault("checks.scaffold.godbridge-min-pitch", 70.0);
        cfg.addDefault("checks.scaffold.godbridge-max-pitch", 85.5);
        cfg.addDefault("checks.scaffold.max-blocks-per-second", 13);
        cfg.addDefault("checks.scaffold.max-rotation-mismatch-angle", 105.0);
        cfg.addDefault("checks.scaffold.impossible-snap-yaw", 125.0);
        cfg.addDefault("checks.scaffold.buffer-max", 3.0);
        cfg.addDefault("checks.scaffold.buffer-decay", 0.35);

        cfg.addDefault("checks.fastbreak.speed-Ratio-threshold", 0.78);
        cfg.addDefault("checks.fastbreak.buffer-max", 2.0);
        cfg.addDefault("checks.fastbreak.buffer-decay", 0.25);

        cfg.addDefault("checks.bed-through-wall.ray-step", 0.15);
        cfg.addDefault("checks.bed-through-wall.max-reach", 5.2);
        cfg.addDefault("checks.bed-through-wall.buffer-max", 1.0);
        cfg.addDefault("checks.bed-through-wall.buffer-decay", 0.2);

        cfg.addDefault("checks.nuker.enabled", true);
        cfg.addDefault("checks.nuker.max-vl", 5);
        cfg.addDefault("checks.nuker.punish", true);
        cfg.addDefault("checks.nuker.reason", "Unfair Advantage (World/Nuker)");
        cfg.addDefault("checks.nuker.max-breaks-per-second", 12);
        cfg.addDefault("checks.nuker.max-angle", 105.0);
        cfg.addDefault("checks.nuker.buffer-max", 2.0);
        cfg.addDefault("checks.nuker.buffer-decay", 0.25);

        cfg.addDefault("checks.cheststealer.enabled", true);
        cfg.addDefault("checks.cheststealer.max-vl", 8);
        cfg.addDefault("checks.cheststealer.punish", true);
        cfg.addDefault("checks.cheststealer.reason", "Unfair Advantage (Player/ChestStealer)");
        cfg.addDefault("checks.cheststealer.min-click-interval-ms", 42L);
        cfg.addDefault("checks.cheststealer.sample-size", 6);
        cfg.addDefault("checks.cheststealer.buffer-max", 3.0);
        cfg.addDefault("checks.cheststealer.buffer-decay", 0.5);

        cfg.addDefault("checks.invmove.enabled", true);
        cfg.addDefault("checks.invmove.max-vl", 6);
        cfg.addDefault("checks.invmove.punish", true);
        cfg.addDefault("checks.invmove.reason", "Unfair Advantage (Movement/InvMove)");
        cfg.addDefault("checks.invmove.min-walk-speed", 0.16);
        cfg.addDefault("checks.invmove.buffer-max", 3.0);
        cfg.addDefault("checks.invmove.buffer-decay", 0.4);

        cfg.addDefault("checks.fastuse.enabled", true);
        cfg.addDefault("checks.fastuse.max-vl", 6);
        cfg.addDefault("checks.fastuse.punish", true);
        cfg.addDefault("checks.fastuse.reason", "Unfair Advantage (Player/FastUse)");
        cfg.addDefault("checks.fastuse.min-consume-ms", 1300L);

        cfg.addDefault("checks.aim-modulo360.enabled", true);
        cfg.addDefault("checks.aim-modulo360.max-vl", 8);
        cfg.addDefault("checks.aim-modulo360.punish", true);
        cfg.addDefault("checks.aim-modulo360.reason", "Unfair Advantage (Grim/AimModulo360)");
        cfg.addDefault("checks.aim-modulo360.buffer-max", 3.0);
        cfg.addDefault("checks.aim-modulo360.buffer-decay", 0.25);

        cfg.addDefault("checks.badpackets.enabled", true);
        cfg.addDefault("checks.badpackets.max-vl", 4);
        cfg.addDefault("checks.badpackets.punish", true);
        cfg.addDefault("checks.badpackets.reason", "Unfair Advantage (Grim/BadPackets)");
        cfg.addDefault("checks.badpackets.buffer-max", 2.0);
        cfg.addDefault("checks.badpackets.buffer-decay", 0.25);
        cfg.addDefault("checks.fastuse.buffer-max", 2.0);
        cfg.addDefault("checks.fastuse.buffer-decay", 0.25);
    }

    private int parseOffenseIndex(String key) {
        String digits = key.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }


    public boolean setConfigValue(String path, String rawValue) {
        FileConfiguration cfg = plugin.getConfig();
        if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
            cfg.set(path, Boolean.parseBoolean(rawValue));
        } else {
            try {
                if (rawValue.contains(".")) {
                    cfg.set(path, Double.parseDouble(rawValue));
                } else {
                    cfg.set(path, Integer.parseInt(rawValue));
                }
            } catch (NumberFormatException ex) {
                cfg.set(path, rawValue);
            }
        }
        plugin.saveConfig();
        plugin.reloadAnticheat();
        return true;
    }

    public FileConfiguration getRawConfig() {
        return plugin.getConfig();
    }

    public boolean isCheckEnabled(String checkKey) {
        return plugin.getConfig().getBoolean("checks." + checkKey + ".enabled");
    }

    public int getCheckMaxVl(String checkKey) {
        return plugin.getConfig().getInt("checks." + checkKey + ".max-vl");
    }

    public boolean isCheckPunish(String checkKey) {
        return plugin.getConfig().getBoolean("checks." + checkKey + ".punish");
    }

    public String getCheckReason(String checkKey) {
        return plugin.getConfig().getString("checks." + checkKey + ".reason");
    }

    public double getCheckDouble(String checkKey, String field) {
        return plugin.getConfig().getDouble("checks." + checkKey + "." + field);
    }

    public int getCheckInt(String checkKey, String field) {
        return plugin.getConfig().getInt("checks." + checkKey + "." + field);
    }

    public long getCheckLong(String checkKey, String field) {
        return plugin.getConfig().getLong("checks." + checkKey + "." + field);
    }

    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    public String getAlertsPermission() {
        return alertsPermission;
    }

    public boolean isNotifyOps() {
        return notifyOps;
    }

    public boolean isNotifyConsole() {
        return notifyConsole;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public int getAlertCooldownSeconds() {
        return alertCooldownSeconds;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public boolean isClickTeleport() {
        return clickTeleport;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public String getTeleportCommandPrefix() {
        return teleportCommandPrefix;
    }

    public String getHoverTextFormat() {
        return hoverTextFormat;
    }

    public double getMinTps() {
        return minTps;
    }

    public int getMaxPingExempt() {
        return maxPingExempt;
    }

    public int getVlDecaySeconds() {
        return vlDecaySeconds;
    }

    public int getVlDecayAmount() {
        return vlDecayAmount;
    }

    public double getBufferDecayMultiplier() {
        return bufferDecayMultiplier;
    }

    public String getExemptPermission() {
        return exemptPermission;
    }

    public String getAdminPermission() {
        return adminPermission;
    }

    public boolean isUseSqlite() {
        return useSqlite;
    }

    public String getSqliteFileName() {
        return sqliteFileName;
    }

    public String getFlatFileName() {
        return flatFileName;
    }

    public Map<Integer, OffenseStep> getPunishmentLadder() {
        return Collections.unmodifiableMap(punishmentLadder);
    }

    public OffenseStep getOffenseStep(int offenseNumber) {
        if (punishmentLadder.isEmpty()) {
            return new OffenseStep("KICK", "0");
        }
        if (punishmentLadder.containsKey(offenseNumber)) {
            return punishmentLadder.get(offenseNumber);
        }
        int maxKey = 1;
        for (Integer k : punishmentLadder.keySet()) {
            if (k > maxKey) {
                maxKey = k;
            }
        }
        return punishmentLadder.get(maxKey);
    }

    public long getResetOffensesAfterMillis() {
        return resetOffensesAfterMillis;
    }

    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public String getKickCommand() {
        return kickCommand;
    }

    public String getTempbanCommand() {
        return tempbanCommand;
    }

    public String getBanCommand() {
        return banCommand;
    }

    public List<String> getScreenMessage() {
        return screenMessage;
    }

    public int getExemptRespawnTicks() {
        return exemptRespawnTicks;
    }

    public int getExemptExplosionKnockbackTicks() {
        return exemptExplosionKnockbackTicks;
    }

    public int getExemptVoidTeleportTicks() {
        return exemptVoidTeleportTicks;
    }

    public boolean isExemptSpectators() {
        return exemptSpectators;
    }

    public static class OffenseStep {
        private final String type;
        private final String duration;

        public OffenseStep(String type, String duration) {
            this.type = type != null ? type.toUpperCase() : "KICK";
            this.duration = duration != null ? duration : "0";
        }

        public String getType() {
            return type;
        }

        public String getDuration() {
            return duration;
        }
    }
}
