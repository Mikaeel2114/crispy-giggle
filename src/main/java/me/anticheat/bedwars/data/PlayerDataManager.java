package me.anticheat.bedwars.data;

import me.anticheat.bedwars.BedwarsACPlugin;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final BedwarsACPlugin plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<UUID, PlayerData>();

    public PlayerDataManager(BedwarsACPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerData getOrCreate(Player player) {
        PlayerData existing = dataMap.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }
        PlayerData created = new PlayerData(player);
        dataMap.put(player.getUniqueId(), created);
        return created;
    }

    public PlayerData get(Player player) {
        if (player == null) {
            return null;
        }
        return dataMap.get(player.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public Collection<PlayerData> getAllData() {
        return dataMap.values();
    }

    public void clearAll() {
        dataMap.clear();
    }
}
