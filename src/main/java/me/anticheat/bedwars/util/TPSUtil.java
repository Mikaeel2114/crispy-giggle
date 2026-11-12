package me.anticheat.bedwars.util;

import me.anticheat.bedwars.BedwarsACPlugin;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TPSUtil {

    private static volatile double rollingTps = 20.0D;
    private static long lastTickTimestamp = System.currentTimeMillis();

    public static BukkitTask startTask(BedwarsACPlugin plugin) {
        lastTickTimestamp = System.currentTimeMillis();
        return Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long elapsed = now - lastTickTimestamp;
                lastTickTimestamp = now;
                if (elapsed > 0) {
                    double instantTps = Math.min(20.0D, 1000.0D / (elapsed / 20.0D));
                    rollingTps = (rollingTps * 0.85D) + (instantTps * 0.15D);
                }
            }
        }, 20L, 20L);
    }

    public static double getTPS() {
        try {
            double[] recent = MinecraftServer.getServer().recentTps;
            if (recent != null && recent.length > 0) {
                return Math.min(20.0D, Math.round(recent[0] * 100.0D) / 100.0D);
            }
        } catch (Throwable ignored) {
        }
        return Math.min(20.0D, Math.round(rollingTps * 100.0D) / 100.0D);
    }

    public static int getPing(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            return Math.max(0, nmsPlayer.ping);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
