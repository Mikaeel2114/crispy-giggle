package me.anticheat.bedwars.check;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.config.ConfigManager;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.TPSUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public abstract class Check {

    protected final BedwarsACPlugin plugin;
    private final String configKey;
    private final String displayName;

    private boolean enabled;
    private int maxVl;
    private boolean punish;
    private String reason;
    private double bufferMax;
    private double bufferDecay;

    public Check(BedwarsACPlugin plugin, String configKey, String displayName) {
        this.plugin = plugin;
        this.configKey = configKey;
        this.displayName = displayName;
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        ConfigManager cfg = plugin.getConfigManager();
        this.enabled = cfg.isCheckEnabled(configKey);
        this.maxVl = cfg.getCheckMaxVl(configKey);
        this.punish = cfg.isCheckPunish(configKey);
        this.reason = cfg.getCheckReason(configKey);
        this.bufferMax = cfg.getCheckDouble(configKey, "buffer-max");
        this.bufferDecay = cfg.getCheckDouble(configKey, "buffer-decay");
    }

    public boolean isExempt(Player player, PlayerData data) {
        if (!enabled || player == null || data == null) {
            return true;
        }
        ConfigManager cfg = plugin.getConfigManager();
        if (data.isManualExempt() || player.hasPermission(cfg.getExemptPermission())) {
            return true;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode().name().equals("SPECTATOR")) {
            return true;
        }
        if (plugin.getBedwarsHook().isSpectatingOrRespawning(player)) {
            return true;
        }
        if (data.getRespawnExemptTicks() > 0 || data.getVoidTeleportExemptTicks() > 0) {
            return true;
        }
        if (TPSUtil.getTPS() < cfg.getMinTps()) {
            return true;
        }
        if (TPSUtil.getPing(player) > cfg.getMaxPingExempt()) {
            return true;
        }
        return false;
    }

    protected void fail(Player player, PlayerData data, double bufferWeight, String details) {
        if (isExempt(player, data)) {
            return;
        }
        double currentBuffer = data.incrementBuffer(configKey, bufferWeight);
        if (currentBuffer >= Math.max(1.0D, bufferMax)) {
            data.resetBuffer(configKey);
            int vl = data.incrementVl(configKey);
            plugin.getAlertManager().handleAlert(player, data, this, vl, details);

            if (punish && maxVl > 0 && vl >= maxVl) {
                plugin.getPunishmentManager().punish(player, data, this);
            }
        }
    }

    protected void reward(PlayerData data) {
        if (data != null && bufferDecay > 0.0D) {
            data.decayBuffer(configKey, bufferDecay);
        }
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxVl() {
        return maxVl;
    }

    public boolean isPunish() {
        return punish;
    }

    public String getReason() {
        return reason;
    }
}
