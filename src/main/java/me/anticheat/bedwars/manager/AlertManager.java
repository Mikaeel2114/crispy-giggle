package me.anticheat.bedwars.manager;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.config.ConfigManager;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.TPSUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AlertManager {

    private final BedwarsACPlugin plugin;

    public AlertManager(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleAlert(Player suspect, PlayerData suspectData, Check check, int vl, String details) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isAlertsEnabled()) {
            return;
        }

        int ping = TPSUtil.getPing(suspect);
        double tps = TPSUtil.getTPS();

        String formatted = cfg.getAlertMessage()
                .replace("%player%", suspect.getName())
                .replace("%check%", check.getDisplayName())
                .replace("%vl%", String.valueOf(vl))
                .replace("%max%", String.valueOf(check.getMaxVl()))
                .replace("%ping%", String.valueOf(ping))
                .replace("%tps%", String.format("%.2f", tps));

        if (cfg.isNotifyConsole()) {
            Bukkit.getConsoleSender().sendMessage(formatted + (details != null && !details.isEmpty() ? " (" + details + ")" : ""));
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cfg.getAlertCooldownSeconds() * 1000L;

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(formatted));
        if (cfg.isClickTeleport()) {
            String tpCmd = cfg.getTeleportCommandPrefix().replace("%player%", suspect.getName());
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd));
            String hoverText = cfg.getHoverTextFormat()
                    .replace("%player%", suspect.getName())
                    .replace("%details%", details != null ? details : "None");
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        }

        Sound alertSound = parseSound(cfg.getAlertSound());

        for (Player online : Bukkit.getOnlinePlayers()) {
            boolean hasPerm = online.hasPermission(cfg.getAlertsPermission())
                    || (cfg.isNotifyOps() && online.isOp());
            if (!hasPerm) {
                continue;
            }

            PlayerData staffData = plugin.getPlayerDataManager().get(online);
            if (staffData == null || !staffData.isAlertsEnabled()) {
                continue;
            }

            if (!staffData.isDebugEnabled() && (now - staffData.getLastAlertSentMillis() < cooldownMillis)) {
                continue;
            }
            staffData.setLastAlertSentMillis(now);

            String outMsg = formatted;
            if (staffData.isDebugEnabled() && details != null && !details.isEmpty()) {
                outMsg = formatted + ConfigManager.color(" &8[&7" + details + "&8]");
            }

            if (cfg.isClickTeleport()) {
                if (staffData.isDebugEnabled() && details != null && !details.isEmpty()) {
                    TextComponent debugComp = new TextComponent(TextComponent.fromLegacyText(outMsg));
                    debugComp.setClickEvent(component.getClickEvent());
                    debugComp.setHoverEvent(component.getHoverEvent());
                    online.spigot().sendMessage(debugComp);
                } else {
                    online.spigot().sendMessage(component);
                }
            } else {
                online.sendMessage(outMsg);
            }

            if (alertSound != null) {
                online.playSound(online.getLocation(), alertSound, 1.0F, 1.0F);
            }
        }

        // Log violation asynchronously to storage
        plugin.getPunishmentManager().logViolationAsync(
                suspect.getUniqueId(),
                suspect.getName(),
                check.getDisplayName(),
                vl,
                check.getMaxVl(),
                ping,
                tps,
                details
        );

        // Optional Discord webhook
        final String webhookUrl = cfg.getDiscordWebhook();
        if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            sendDiscordWebhookAsync(webhookUrl.trim(), suspect.getName(), check.getDisplayName(), vl, check.getMaxVl(), ping, tps, details);
        }
    }

    private Sound parseSound(String soundName) {
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void sendDiscordWebhookAsync(final String urlString, final String player, final String check,
                                        final int vl, final int maxVl, final int ping, final double tps, final String details) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("User-Agent", "BedwarsAC-Webhook/1.0");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    String safeDetails = details == null ? "" : details.replace("\"", "'");
                    String jsonPayload = "{"
                            + "\"username\":\"BedwarsAC\","
                            + "\"embeds\":[{"
                            + "\"title\":\"Anticheat Violation Alert\","
                            + "\"color\":16724787,"
                            + "\"fields\":["
                            + "{\"name\":\"Player\",\"value\":\"" + player + "\",\"inline\":true},"
                            + "{\"name\":\"Check\",\"value\":\"" + check + " (" + vl + "/" + maxVl + ")\",\"inline\":true},"
                            + "{\"name\":\"Ping / TPS\",\"value\":\"" + ping + "ms / " + String.format("%.2f", tps) + "\",\"inline\":true},"
                            + "{\"name\":\"Details\",\"value\":\"" + (safeDetails.isEmpty() ? "N/A" : safeDetails) + "\",\"inline\":false}"
                            + "]"
                            + "}]"
                            + "}";

                    byte[] out = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    OutputStream os = conn.getOutputStream();
                    os.write(out);
                    os.flush();
                    os.close();

                    conn.getResponseCode();
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }
}
