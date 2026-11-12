package me.anticheat.bedwars;

import me.anticheat.bedwars.check.CheckManager;
import me.anticheat.bedwars.command.ACCommand;
import me.anticheat.bedwars.config.ConfigManager;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.data.PlayerDataManager;
import me.anticheat.bedwars.hook.BedwarsHook;
import me.anticheat.bedwars.listener.PlayerListener;
import me.anticheat.bedwars.manager.AlertManager;
import me.anticheat.bedwars.manager.PunishmentManager;
import me.anticheat.bedwars.packet.PacketInterceptor;
import me.anticheat.bedwars.util.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BedwarsACPlugin extends JavaPlugin {

    private static BedwarsACPlugin instance;

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private AlertManager alertManager;
    private PunishmentManager punishmentManager;
    private CheckManager checkManager;
    private PacketInterceptor packetInterceptor;
    private BedwarsHook bedwarsHook;
    private BukkitTask decayTask;
    private BukkitTask tpsTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.tpsTask = TPSUtil.startTask(this);

        this.playerDataManager = new PlayerDataManager(this);
        this.alertManager = new AlertManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.bedwarsHook = new BedwarsHook(this);
        this.checkManager = new CheckManager(this);
        this.packetInterceptor = new PacketInterceptor(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        this.bedwarsHook.registerHooks();

        ACCommand acCommand = new ACCommand(this);
        if (getCommand("ac") != null) {
            getCommand("ac").setExecutor(acCommand);
            getCommand("ac").setTabCompleter(acCommand);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerData data = playerDataManager.getOrCreate(online);
            packetInterceptor.inject(online, data);
        }

        startDecayTask();
        getLogger().info("BedwarsAC v" + getDescription().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (decayTask != null) {
            decayTask.cancel();
        }
        if (tpsTask != null) {
            tpsTask.cancel();
        }
        if (packetInterceptor != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                packetInterceptor.uninject(online);
            }
            packetInterceptor.shutdown();
        }
        if (punishmentManager != null) {
            punishmentManager.close();
        }
        if (playerDataManager != null) {
            playerDataManager.clearAll();
        }
        instance = null;
    }

    public void reloadAnticheat() {
        reloadConfig();
        this.configManager.load();
        this.checkManager.reloadChecks();
        startDecayTask();
    }

    private void startDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
        }
        long periodTicks = Math.max(20L, configManager.getVlDecaySeconds() * 20L);
        this.decayTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                for (PlayerData data : playerDataManager.getAllData()) {
                    data.decayViolations(configManager.getVlDecayAmount(), configManager.getBufferDecayMultiplier());
                }
            }
        }, periodTicks, periodTicks);
    }

    public static BedwarsACPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public PacketInterceptor getPacketInterceptor() {
        return packetInterceptor;
    }

    public BedwarsHook getBedwarsHook() {
        return bedwarsHook;
    }
}
