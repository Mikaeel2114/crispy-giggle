package me.anticheat.bedwars.util;

import org.bukkit.Location;

public class TimedLocation {

    private final Location location;
    private final BoundingBox boundingBox;
    private final long timestamp;

    public TimedLocation(Location location, long timestamp) {
        this.location = location.clone();
        this.boundingBox = BoundingBox.fromPlayerLocation(this.location);
        this.timestamp = timestamp;
    }

    public Location getLocation() {
        return location;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
