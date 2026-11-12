package me.anticheat.bedwars.command;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.config.ConfigManager;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ACCommand implements CommandExecutor, TabCompleter {

    private final BedwarsACPlugin plugin;

    public ACCommand(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("alerts")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ConfigManager.color("&cOnly players can toggle personal alerts."));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission(cfg.getAlertsPermission()) && !player.hasPermission(cfg.getAdminPermission()) && !player.isOp()) {
                player.sendMessage(ConfigManager.color("&cYou do not have permission to use this command."));
                return true;
            }
            PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
            boolean next = !data.isAlertsEnabled();
            data.setAlertsEnabled(next);
            player.sendMessage(ConfigManager.color("&8[&cAC&8] &7Anticheat alerts are now " + (next ? "&aENABLED" : "&cDISABLED") + "&7."));
            return true;
        }

        if (!sender.hasPermission(cfg.getAdminPermission()) && !sender.isOp()) {
            sender.sendMessage(ConfigManager.color("&cYou do not have permission to use this command."));
            return true;
        }

        if (sub.equals("cps")) {
            if (args.length < 2) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac cps <player> [watchSeconds]"));
                return true;
            }
            final Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ConfigManager.color("&cPlayer not found or offline."));
                return true;
            }
            final PlayerData data = plugin.getPlayerDataManager().getOrCreate(target);
            int watchSeconds = 1;
            if (args.length >= 3) {
                try {
                    watchSeconds = Math.max(1, Math.min(15, Integer.parseInt(args[2])));
                } catch (NumberFormatException ignored) {
                }
            }

            if (watchSeconds <= 1) {
                sender.sendMessage(ConfigManager.color("&8[&cAC&8] &f" + target.getName()
                        + " &7CPS: &a" + data.getCurrentCps()
                        + " &8| &7Sustained (2s): &f" + String.format("%.1f", data.getSustainedCps(2))
                        + " &8| &7Peak: &c" + data.getPeakCps()
                        + " &8| &7StdDev: &f" + String.format("%.2fms", data.getLastStdDevMs())
                        + " &8| &7Ping: &f" + TPSUtil.getPing(target) + "ms"));
                return true;
            }

            final int totalSeconds = watchSeconds;
            sender.sendMessage(ConfigManager.color("&8[&cAC&8] &7Monitoring &f" + target.getName() + "&7's CPS for &a" + totalSeconds + "s&7..."));
            new BukkitRunnable() {
                private int elapsed = 0;

                @Override
                public void run() {
                    if (!target.isOnline() || elapsed >= totalSeconds) {
                        cancel();
                        return;
                    }
                    elapsed++;
                    sender.sendMessage(ConfigManager.color("&8[&cCPS " + elapsed + "/" + totalSeconds + "s&8] &f" + target.getName()
                            + " &7Now: &a" + data.getCurrentCps() + " CPS"
                            + " &8| &7Avg(2s): &f" + String.format("%.1f", data.getSustainedCps(2))
                            + " &8| &7Peak: &c" + data.getPeakCps()
                            + " &8| &7StdDev: &f" + String.format("%.2fms", data.getLastStdDevMs())));
                }
            }.runTaskTimer(plugin, 20L, 20L);
            return true;
        }

        if (sub.equals("settings") || sub.equals("set")) {
            if (sub.equals("settings") && args.length == 1) {
                sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
                sender.sendMessage(ConfigManager.color("&c&lBedwarsAC Check Settings &8(&7Use &f/ac set <check> <key> <value>&8)"));
                for (Check check : plugin.getCheckManager().getAllChecks()) {
                    sender.sendMessage(ConfigManager.color("  &8- &f" + check.getConfigKey()
                            + " &8[" + (check.isEnabled() ? "&aON" : "&cOFF") + "&8]"
                            + " &7MaxVL: &f" + check.getMaxVl()
                            + " &7Punish: &f" + check.isPunish()));
                }
                sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
                return true;
            }
            if (sub.equals("settings") && args.length == 2) {
                String checkKey = args[1].toLowerCase(Locale.ROOT);
                sender.sendMessage(ConfigManager.color("&8[&cAC&8] &7Settings for &cchecks." + checkKey + "&7:"));
                sender.sendMessage(ConfigManager.color("  &7enabled: &f" + cfg.isCheckEnabled(checkKey)
                        + " &8| &7max-vl: &f" + cfg.getCheckMaxVl(checkKey)
                        + " &8| &7punish: &f" + cfg.isCheckPunish(checkKey)
                        + " &8| &7buffer-max: &f" + cfg.getCheckDouble(checkKey, "buffer-max")
                        + " &8| &7buffer-decay: &f" + cfg.getCheckDouble(checkKey, "buffer-decay")));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac set <check|general> <parameter> <value>"));
                return true;
            }
            String sectionOrCheck = args[1].toLowerCase(Locale.ROOT);
            String param = args[2].toLowerCase(Locale.ROOT);
            StringBuilder valBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                valBuilder.append(args[i]).append(i < args.length - 1 ? " " : "");
            }
            String value = valBuilder.toString();
            String fullPath = (sectionOrCheck.equals("general") || sectionOrCheck.equals("alerts") || sectionOrCheck.equals("bedwars"))
                    ? (sectionOrCheck + "." + param)
                    : ("checks." + sectionOrCheck + "." + param);

            cfg.setConfigValue(fullPath, value);
            sender.sendMessage(ConfigManager.color("&8[&cAC&8] &aUpdated &f" + fullPath + " &7= &e" + value + " &a(saved & reloaded)."));
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadAnticheat();
            sender.sendMessage(ConfigManager.color("&8[&cAC&8] &aConfiguration and all check thresholds reloaded."));
            return true;
        }

        if (sub.equals("debug")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ConfigManager.color("&cOnly players can toggle debug mode."));
                return true;
            }
            Player player = (Player) sender;
            PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
            boolean next = !data.isDebugEnabled();
            data.setDebugEnabled(next);
            player.sendMessage(ConfigManager.color("&8[&cAC&8] &7Verbose debug mode is now " + (next ? "&aENABLED" : "&cDISABLED") + "&7."));
            return true;
        }

        if (sub.equals("info")) {
            if (args.length < 2) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac info <player>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ConfigManager.color("&cPlayer not found or offline."));
                return true;
            }
            PlayerData data = plugin.getPlayerDataManager().getOrCreate(target);
            sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
            sender.sendMessage(ConfigManager.color("&c&lBedwarsAC Info &8- &f" + target.getName()));
            sender.sendMessage(ConfigManager.color("&7Ping: &f" + TPSUtil.getPing(target) + "ms &8| &7Server TPS: &f" + String.format("%.2f", TPSUtil.getTPS())));
            sender.sendMessage(ConfigManager.color("&7CPS (Now/2s/Peak): &a" + data.getCurrentCps() + " &7/ &f"
                    + String.format("%.1f", data.getSustainedCps(2)) + " &7/ &c" + data.getPeakCps()));
            sender.sendMessage(ConfigManager.color("&7Exempt: &f" + (data.isManualExempt() || target.hasPermission(cfg.getExemptPermission()))
                    + " &8| &7Bedwars Spectating: &f" + plugin.getBedwarsHook().isSpectatingOrRespawning(target)));
            Map<String, Integer> vls = data.getAllViolations();
            if (vls.isEmpty()) {
                sender.sendMessage(ConfigManager.color("&7Active Violations: &aNone"));
            } else {
                sender.sendMessage(ConfigManager.color("&7Active Violations:"));
                for (Map.Entry<String, Integer> entry : vls.entrySet()) {
                    sender.sendMessage(ConfigManager.color("  &8- &c" + entry.getKey() + "&7: VL &f" + entry.getValue()));
                }
            }
            sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
            return true;
        }

        if (sub.equals("logs")) {
            if (args.length < 2) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac logs <player>"));
                return true;
            }
            String targetName = args[1];
            List<String> logs = plugin.getPunishmentManager().getRecentLogs(targetName, 12);
            sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
            sender.sendMessage(ConfigManager.color("&c&lRecent Violation Logs &8- &f" + targetName));
            if (logs.isEmpty()) {
                sender.sendMessage(ConfigManager.color("&7No violation logs recorded for this player."));
            } else {
                for (String logLine : logs) {
                    sender.sendMessage(ConfigManager.color(logLine));
                }
            }
            sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
            return true;
        }

        if (sub.equals("ban")) {
            if (args.length < 3) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac ban <player> <reason>"));
                return true;
            }
            String targetName = args[1];
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(i < args.length - 1 ? " " : "");
            }
            String reason = reasonBuilder.toString();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offline != null ? offline.getUniqueId() : null;
            String banId = plugin.getPunishmentManager().manualBan(targetName, uuid, reason);
            sender.sendMessage(ConfigManager.color("&8[&cAC&8] &7Banned &f" + targetName + " &7(ID: &f" + banId + "&7) for: &f" + reason));
            return true;
        }

        if (sub.equals("unban")) {
            if (args.length < 2) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac unban <player>"));
                return true;
            }
            String targetName = args[1];
            boolean unbanned = plugin.getPunishmentManager().unbanPlayer(targetName);
            if (unbanned) {
                sender.sendMessage(ConfigManager.color("&8[&cAC&8] &aSuccessfully unbanned &f" + targetName + "&a."));
            } else {
                sender.sendMessage(ConfigManager.color("&8[&cAC&8] &cNo active anticheat ban found for &f" + targetName + "&c."));
            }
            return true;
        }

        if (sub.equals("exempt")) {
            if (args.length < 2) {
                sender.sendMessage(ConfigManager.color("&cUsage: /ac exempt <player>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ConfigManager.color("&cPlayer must be online to toggle manual exemption."));
                return true;
            }
            PlayerData data = plugin.getPlayerDataManager().getOrCreate(target);
            boolean next = !data.isManualExempt();
            data.setManualExempt(next);
            sender.sendMessage(ConfigManager.color("&8[&cAC&8] &7Exemption for &f" + target.getName() + " &7is now "
                    + (next ? "&aENABLED" : "&cDISABLED") + "&7."));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
        sender.sendMessage(ConfigManager.color("&c&lBedwarsAC Commands"));
        sender.sendMessage(ConfigManager.color("&c/ac cps <player> [seconds] &8- &7Check or monitor live CPS & StdDev"));
        sender.sendMessage(ConfigManager.color("&c/ac settings [check] &8- &7Inspect all check settings in-game"));
        sender.sendMessage(ConfigManager.color("&c/ac set <check> <key> <value> &8- &7Modify any check threshold live"));
        sender.sendMessage(ConfigManager.color("&c/ac alerts &8- &7Toggle real-time staff violation alerts"));
        sender.sendMessage(ConfigManager.color("&c/ac info <player> &8- &7View ping, TPS, CPS, exemptions, and VLs"));
        sender.sendMessage(ConfigManager.color("&c/ac logs <player> &8- &7View historical violation logs"));
        sender.sendMessage(ConfigManager.color("&c/ac ban <player> <reason> &8- &7Manually ban a player"));
        sender.sendMessage(ConfigManager.color("&c/ac unban <player> &8- &7Remove an active anticheat ban"));
        sender.sendMessage(ConfigManager.color("&c/ac exempt <player> &8- &7Toggle anticheat exemption for a player"));
        sender.sendMessage(ConfigManager.color("&c/ac debug &8- &7Toggle verbose check telemetry on alerts"));
        sender.sendMessage(ConfigManager.color("&c/ac reload &8- &7Reload config.yml and all check thresholds"));
        sender.sendMessage(ConfigManager.color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("cps", "settings", "set", "reload", "alerts", "info", "logs", "ban", "unban", "exempt", "debug");
            List<String> matches = new ArrayList<String>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : subs) {
                if (s.startsWith(prefix)) {
                    matches.add(s);
                }
            }
            return matches;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<String>();
            if (sub.equals("set") || sub.equals("settings")) {
                for (Check check : plugin.getCheckManager().getAllChecks()) {
                    if (check.getConfigKey().startsWith(prefix)) {
                        matches.add(check.getConfigKey());
                    }
                }
                return matches;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> params = Arrays.asList("enabled", "max-vl", "punish", "buffer-max", "buffer-decay", "max-cps", "max-reach", "high-ping-reach", "fov-angle", "min-percent");
            List<String> matches = new ArrayList<String>();
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (String p : params) {
                if (p.startsWith(prefix)) {
                    matches.add(p);
                }
            }
            return matches;
        }
        return new ArrayList<String>();
    }
}
