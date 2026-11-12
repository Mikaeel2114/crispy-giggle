package me.anticheat.bedwars.data;

import me.anticheat.bedwars.util.TimedLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final UUID uuid;
    private final String name;

    // Violation levels and buffers per check key
    private final Map<String, Integer> violations = new ConcurrentHashMap<String, Integer>();
    private final Map<String, Double> buffers = new ConcurrentHashMap<String, Double>();

    // Staff / debug flags
    private volatile boolean alertsEnabled = true;
    private volatile boolean debugEnabled = false;
    private volatile boolean manualExempt = false;
    private volatile long lastAlertSentMillis = 0L;

    // Packet & tick timing (Grim-style tick state)
    private volatile long lastFlyingPacketMillis = System.currentTimeMillis();
    private volatile long lastAttackPacketMillis = 0L;
    private volatile boolean sentFlyingSinceLastAttack = true;
    private volatile boolean swungArmThisTick = false;
    private volatile boolean placedBlockThisTick = false;
    private volatile int attacksThisTick = 0;
    private volatile int currentClientTick = 0;
    private final Set<Integer> attackedEntitiesThisTick = Collections.synchronizedSet(new HashSet<Integer>());
    private final Deque<Long> flyingSamples = new ArrayDeque<Long>();
    private volatile double timerBalanceMs = 0.0D;
    private volatile long lastTimerCheckMs = 0L;

    // AutoClicker & Live CPS state
    private final Deque<Long> clickTimestamps = new ArrayDeque<Long>();
    private final Deque<Long> clickIntervals = new ArrayDeque<Long>();
    private volatile long lastArmAnimationMillis = 0L;
    private volatile double lastStdDevMs = 0.0D;
    private volatile int peakCps = 0;
    private volatile boolean diggingBlock = false;
    private volatile long lastDigPacketMillis = 0L;
    private volatile long lastBlockPlaceMillis = 0L;

    // Rotation & Movement state
    private volatile Location lastLocation;
    private volatile Location currentLocation;
    private volatile double deltaXZ = 0.0D;
    private volatile double lastDeltaXZ = 0.0D;
    private volatile double deltaY = 0.0D;
    private volatile double lastDeltaY = 0.0D;
    private volatile float rawDeltaYaw = 0.0F;
    private volatile float lastRawDeltaYaw = 0.0F;
    private volatile float deltaYaw = 0.0F;
    private volatile float lastDeltaYaw = 0.0F;
    private volatile float deltaPitch = 0.0F;
    private volatile float lastDeltaPitch = 0.0F;
    private volatile boolean clientOnGround = true;
    private volatile boolean serverOnGround = true;
    private volatile boolean lastServerOnGround = true;
    private volatile int airTicks = 0;
    private volatile int groundTicks = 0;
    private volatile int ticksSinceAttack = 100;
    private volatile double fallDistance = 0.0D;
    private volatile boolean sprinting = false;
    private volatile boolean sneaking = false;
    private volatile boolean inventoryOpen = false;
    private volatile long inventoryOpenMillis = 0L;
    private volatile long itemUseStartMillis = 0L;

    // Environment & Exemption tick counters
    private volatile int iceTicks = 0;
    private volatile int slimeTicks = 0;
    private volatile int stairSlabTicks = 0;
    private volatile int soulSandTicks = 0;
    private volatile int liquidTicks = 0;
    private volatile int climbableTicks = 0;
    private volatile int webTicks = 0;
    private volatile int underBlockTicks = 0;
    private volatile int velocityTicks = 0;
    private volatile int teleportTicks = 0;
    private volatile int respawnExemptTicks = 0;
    private volatile int explosionExemptTicks = 0;
    private volatile int voidTeleportExemptTicks = 0;

    // Velocity / AntiKB tracking
    private volatile double pendingVelocityX = 0.0D;
    private volatile double pendingVelocityY = 0.0D;
    private volatile double pendingVelocityZ = 0.0D;
    private volatile double pendingVelocityHorizontal = 0.0D;
    private volatile long velocityReceivedMillis = 0L;
    private volatile boolean waitingForVelocityResponse = false;
    private volatile int lowVelocityStreak = 0;

    // FastBreak / Scaffold / ChestStealer tracking
    private volatile long blockBreakStartMillis = 0L;
    private volatile Material breakingMaterial = null;
    private final Deque<Long> scaffoldPlaceTimestamps = new ArrayDeque<Long>();
    private final Deque<Long> blockBreakTimestamps = new ArrayDeque<Long>();
    private final Deque<Long> chestClickTimestamps = new ArrayDeque<Long>();
    private volatile int lastChestSlot = -1;

    // Target backtrack history (stores positions of this player so attackers can raytrace against it)
    private final Deque<TimedLocation> locationHistory = new ArrayDeque<TimedLocation>();

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.lastLocation = player.getLocation().clone();
        this.currentLocation = player.getLocation().clone();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getVl(String checkKey) {
        Integer val = violations.get(checkKey);
        return val != null ? val : 0;
    }

    public int incrementVl(String checkKey) {
        int next = getVl(checkKey) + 1;
        violations.put(checkKey, next);
        return next;
    }

    public void resetVl() {
        violations.clear();
        buffers.clear();
    }

    public Map<String, Integer> getAllViolations() {
        return Collections.unmodifiableMap(violations);
    }

    public double getBuffer(String checkKey) {
        Double buf = buffers.get(checkKey);
        return buf != null ? buf : 0.0D;
    }

    public double incrementBuffer(String checkKey, double amount) {
        double next = getBuffer(checkKey) + amount;
        buffers.put(checkKey, next);
        return next;
    }

    public void decayBuffer(String checkKey, double decayAmount) {
        double next = Math.max(0.0D, getBuffer(checkKey) - decayAmount);
        buffers.put(checkKey, next);
    }

    public void resetBuffer(String checkKey) {
        buffers.put(checkKey, 0.0D);
    }

    public void decayViolations(int vlDecayAmount, double bufferMultiplier) {
        for (Map.Entry<String, Integer> entry : violations.entrySet()) {
            int updated = Math.max(0, entry.getValue() - vlDecayAmount);
            if (updated == 0) {
                violations.remove(entry.getKey());
            } else {
                violations.put(entry.getKey(), updated);
            }
        }
        for (Map.Entry<String, Double> entry : buffers.entrySet()) {
            double updated = entry.getValue() * bufferMultiplier;
            if (updated < 0.05D) {
                buffers.remove(entry.getKey());
            } else {
                buffers.put(entry.getKey(), updated);
            }
        }
    }

    public void recordClick(long now) {
        synchronized (clickTimestamps) {
            clickTimestamps.addLast(now);
            while (!clickTimestamps.isEmpty() && (now - clickTimestamps.peekFirst() > 2000L)) {
                clickTimestamps.removeFirst();
            }
            int currentOneSec = 0;
            for (Long ts : clickTimestamps) {
                if (now - ts <= 1000L) {
                    currentOneSec++;
                }
            }
            if (currentOneSec > peakCps) {
                peakCps = currentOneSec;
            }
        }
    }

    public int getCurrentCps() {
        long now = System.currentTimeMillis();
        int count = 0;
        synchronized (clickTimestamps) {
            for (Long ts : clickTimestamps) {
                if (now - ts <= 1000L) {
                    count++;
                }
            }
        }
        return count;
    }

    public double getSustainedCps(int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, windowSeconds) * 1000L;
        synchronized (clickTimestamps) {
            while (!clickTimestamps.isEmpty() && (now - clickTimestamps.peekFirst() > windowMillis)) {
                clickTimestamps.removeFirst();
            }
            if (clickTimestamps.isEmpty() || (now - clickTimestamps.peekFirst() < (windowMillis - 250L))) {
                return 0.0D;
            }
            return clickTimestamps.size() / (double) windowSeconds;
        }
    }

    public int getPeakCps() {
        return peakCps;
    }

    public double getLastStdDevMs() {
        return lastStdDevMs;
    }

    public void setLastStdDevMs(double lastStdDevMs) {
        this.lastStdDevMs = lastStdDevMs;
    }

    public void recordLocationHistory(Location loc) {
        long now = System.currentTimeMillis();
        synchronized (locationHistory) {
            locationHistory.addLast(new TimedLocation(loc, now));
            while (locationHistory.size() > 40) {
                locationHistory.removeFirst();
            }
        }
    }

    public List<TimedLocation> getLocationsNearTimestamp(long targetTimestamp, long windowMillis) {
        List<TimedLocation> matches = new ArrayList<TimedLocation>();
        synchronized (locationHistory) {
            for (TimedLocation tl : locationHistory) {
                if (Math.abs(tl.getTimestamp() - targetTimestamp) <= windowMillis) {
                    matches.add(tl);
                }
            }
            if (matches.isEmpty() && !locationHistory.isEmpty()) {
                matches.add(locationHistory.getLast());
            }
        }
        return matches;
    }

    public void updateMovement(Player player, Location from, Location to, boolean packetGround) {
        this.lastLocation = from.clone();
        this.currentLocation = to.clone();
        this.lastDeltaXZ = this.deltaXZ;
        this.lastDeltaY = this.deltaY;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        this.deltaXZ = Math.hypot(dx, dz);
        this.deltaY = to.getY() - from.getY();

        this.lastRawDeltaYaw = this.rawDeltaYaw;
        this.rawDeltaYaw = to.getYaw() - from.getYaw();

        this.lastDeltaYaw = this.deltaYaw;
        this.lastDeltaPitch = this.deltaPitch;
        this.deltaYaw = Math.abs(wrapAngle180(this.rawDeltaYaw));
        this.deltaPitch = Math.abs(to.getPitch() - from.getPitch());

        this.clientOnGround = packetGround;
        this.lastServerOnGround = this.serverOnGround;
        this.serverOnGround = computeServerGroundAndEnvironment(player, to);

        if (this.serverOnGround) {
            this.groundTicks++;
            this.airTicks = 0;
            this.fallDistance = 0.0D;
        } else {
            this.airTicks++;
            this.groundTicks = 0;
            if (this.deltaY < 0.0D) {
                this.fallDistance += (-this.deltaY);
            }
        }

        this.ticksSinceAttack++;
        if (velocityTicks > 0) velocityTicks--;
        if (teleportTicks > 0) teleportTicks--;
        if (respawnExemptTicks > 0) respawnExemptTicks--;
        if (explosionExemptTicks > 0) explosionExemptTicks--;
        if (voidTeleportExemptTicks > 0) voidTeleportExemptTicks--;

        recordLocationHistory(to);
    }

    private boolean computeServerGroundAndEnvironment(Player player, Location loc) {
        boolean foundGround = false;
        boolean onIce = false;
        boolean onSlime = false;
        boolean onStairOrSlab = false;
        boolean onSoulSand = false;
        boolean inLiquid = false;
        boolean onClimbable = false;
        boolean inWeb = false;
        boolean blockAboveHead = false;

        for (double offsetX = -0.3D; offsetX <= 0.3D; offsetX += 0.3D) {
            for (double offsetZ = -0.3D; offsetZ <= 0.3D; offsetZ += 0.3D) {
                Block below = loc.clone().add(offsetX, -0.08D, offsetZ).getBlock();
                Block deepBelow = loc.clone().add(offsetX, -0.55D, offsetZ).getBlock();
                Block feet = loc.clone().add(offsetX, 0.1D, offsetZ).getBlock();
                Block head = loc.clone().add(offsetX, 1.85D, offsetZ).getBlock();

                Material bMat = below.getType();
                Material dbMat = deepBelow.getType();
                Material fMat = feet.getType();
                Material hMat = head.getType();

                if (bMat.isSolid() || fMat == Material.WATER_LILY || fMat == Material.CARPET || bMat == Material.CARPET) {
                    foundGround = true;
                }
                if (bMat == Material.ICE || bMat == Material.PACKED_ICE || dbMat == Material.ICE || dbMat == Material.PACKED_ICE) {
                    onIce = true;
                }
                if (bMat == Material.SLIME_BLOCK || dbMat == Material.SLIME_BLOCK) {
                    onSlime = true;
                }
                String bName = bMat.name();
                String fName = fMat.name();
                if (bName.contains("STAIRS") || bName.contains("STEP") || bName.contains("SLAB")
                        || fName.contains("STAIRS") || fName.contains("STEP") || fName.contains("SLAB")
                        || bName.contains("FENCE") || bName.contains("WALL") || bMat == Material.BED_BLOCK) {
                    onStairOrSlab = true;
                    foundGround = true;
                }
                if (bMat == Material.SOUL_SAND || fMat == Material.SOUL_SAND) {
                    onSoulSand = true;
                }
                if (feet.isLiquid() || below.isLiquid()) {
                    inLiquid = true;
                }
                if (fMat == Material.LADDER || fMat == Material.VINE || bMat == Material.LADDER || bMat == Material.VINE) {
                    onClimbable = true;
                }
                if (fMat == Material.WEB || hMat == Material.WEB || bMat == Material.WEB) {
                    inWeb = true;
                }
                if (hMat.isSolid()) {
                    blockAboveHead = true;
                }
            }
        }

        this.iceTicks = onIce ? 40 : Math.max(0, this.iceTicks - 1);
        this.slimeTicks = onSlime ? 40 : Math.max(0, this.slimeTicks - 1);
        this.stairSlabTicks = onStairOrSlab ? 20 : Math.max(0, this.stairSlabTicks - 1);
        this.soulSandTicks = onSoulSand ? 20 : Math.max(0, this.soulSandTicks - 1);
        this.liquidTicks = inLiquid ? 20 : Math.max(0, this.liquidTicks - 1);
        this.climbableTicks = onClimbable ? 15 : Math.max(0, this.climbableTicks - 1);
        this.webTicks = inWeb ? 15 : Math.max(0, this.webTicks - 1);
        this.underBlockTicks = blockAboveHead ? 15 : Math.max(0, this.underBlockTicks - 1);

        return foundGround;
    }

    public static float wrapAngle180(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }
        if (angle < -180.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    public void recordVelocity(double vx, double vy, double vz) {
        this.pendingVelocityX = vx;
        this.pendingVelocityY = vy;
        this.pendingVelocityZ = vz;
        this.pendingVelocityHorizontal = Math.hypot(vx, vz);
        this.velocityReceivedMillis = System.currentTimeMillis();
        this.waitingForVelocityResponse = true;
        this.velocityTicks = 25;
    }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public boolean isDebugEnabled() { return debugEnabled; }
    public void setDebugEnabled(boolean debugEnabled) { this.debugEnabled = debugEnabled; }
    public boolean isManualExempt() { return manualExempt; }
    public void setManualExempt(boolean manualExempt) { this.manualExempt = manualExempt; }
    public long getLastAlertSentMillis() { return lastAlertSentMillis; }
    public void setLastAlertSentMillis(long lastAlertSentMillis) { this.lastAlertSentMillis = lastAlertSentMillis; }
    public long getLastFlyingPacketMillis() { return lastFlyingPacketMillis; }
    public void setLastFlyingPacketMillis(long lastFlyingPacketMillis) { this.lastFlyingPacketMillis = lastFlyingPacketMillis; }
    public long getLastAttackPacketMillis() { return lastAttackPacketMillis; }
    public void setLastAttackPacketMillis(long lastAttackPacketMillis) { this.lastAttackPacketMillis = lastAttackPacketMillis; }
    public boolean isSentFlyingSinceLastAttack() { return sentFlyingSinceLastAttack; }
    public void setSentFlyingSinceLastAttack(boolean sentFlyingSinceLastAttack) { this.sentFlyingSinceLastAttack = sentFlyingSinceLastAttack; }
    public boolean isSwungArmThisTick() { return swungArmThisTick; }
    public void setSwungArmThisTick(boolean swungArmThisTick) { this.swungArmThisTick = swungArmThisTick; }
    public boolean isPlacedBlockThisTick() { return placedBlockThisTick; }
    public void setPlacedBlockThisTick(boolean placedBlockThisTick) { this.placedBlockThisTick = placedBlockThisTick; }
    public int getAttacksThisTick() { return attacksThisTick; }
    public void incrementAttacksThisTick() { this.attacksThisTick++; }
    public int getCurrentClientTick() { return currentClientTick; }

    public void incrementClientTick() {
        this.currentClientTick++;
        this.attackedEntitiesThisTick.clear();
        this.attacksThisTick = 0;
        this.placedBlockThisTick = false;
    }

    public Set<Integer> getAttackedEntitiesThisTick() { return attackedEntitiesThisTick; }
    public Deque<Long> getFlyingSamples() { return flyingSamples; }
    public double getTimerBalanceMs() { return timerBalanceMs; }
    public void setTimerBalanceMs(double timerBalanceMs) { this.timerBalanceMs = timerBalanceMs; }
    public long getLastTimerCheckMs() { return lastTimerCheckMs; }
    public void setLastTimerCheckMs(long lastTimerCheckMs) { this.lastTimerCheckMs = lastTimerCheckMs; }
    public Deque<Long> getClickTimestamps() { return clickTimestamps; }
    public Deque<Long> getClickIntervals() { return clickIntervals; }
    public long getLastArmAnimationMillis() { return lastArmAnimationMillis; }
    public void setLastArmAnimationMillis(long lastArmAnimationMillis) { this.lastArmAnimationMillis = lastArmAnimationMillis; }
    public boolean isDiggingBlock() { return diggingBlock; }
    public void setDiggingBlock(boolean diggingBlock) { this.diggingBlock = diggingBlock; }
    public long getLastDigPacketMillis() { return lastDigPacketMillis; }
    public void setLastDigPacketMillis(long lastDigPacketMillis) { this.lastDigPacketMillis = lastDigPacketMillis; }
    public long getLastBlockPlaceMillis() { return lastBlockPlaceMillis; }
    public void setLastBlockPlaceMillis(long lastBlockPlaceMillis) { this.lastBlockPlaceMillis = lastBlockPlaceMillis; }
    public Location getLastLocation() { return lastLocation; }
    public Location getCurrentLocation() { return currentLocation; }
    public double getDeltaXZ() { return deltaXZ; }
    public double getLastDeltaXZ() { return lastDeltaXZ; }
    public double getDeltaY() { return deltaY; }
    public double getLastDeltaY() { return lastDeltaY; }
    public float getRawDeltaYaw() { return rawDeltaYaw; }
    public float getLastRawDeltaYaw() { return lastRawDeltaYaw; }
    public float getDeltaYaw() { return deltaYaw; }
    public float getLastDeltaYaw() { return lastDeltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }
    public float getLastDeltaPitch() { return lastDeltaPitch; }
    public boolean isClientOnGround() { return clientOnGround; }
    public boolean isServerOnGround() { return serverOnGround; }
    public boolean isLastServerOnGround() { return lastServerOnGround; }
    public int getAirTicks() { return airTicks; }
    public int getGroundTicks() { return groundTicks; }
    public int getTicksSinceAttack() { return ticksSinceAttack; }
    public void resetTicksSinceAttack() { this.ticksSinceAttack = 0; }
    public double getFallDistance() { return fallDistance; }
    public void setFallDistance(double fallDistance) { this.fallDistance = fallDistance; }
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    public boolean isInventoryOpen() { return inventoryOpen; }
    public void setInventoryOpen(boolean inventoryOpen) {
        this.inventoryOpen = inventoryOpen;
        if (inventoryOpen) {
            this.inventoryOpenMillis = System.currentTimeMillis();
        }
    }
    public long getInventoryOpenMillis() { return inventoryOpenMillis; }
    public long getItemUseStartMillis() { return itemUseStartMillis; }
    public void setItemUseStartMillis(long itemUseStartMillis) { this.itemUseStartMillis = itemUseStartMillis; }
    public int getIceTicks() { return iceTicks; }
    public int getSlimeTicks() { return slimeTicks; }
    public int getStairSlabTicks() { return stairSlabTicks; }
    public int getSoulSandTicks() { return soulSandTicks; }
    public int getLiquidTicks() { return liquidTicks; }
    public int getClimbableTicks() { return climbableTicks; }
    public int getWebTicks() { return webTicks; }
    public int getUnderBlockTicks() { return underBlockTicks; }
    public int getVelocityTicks() { return velocityTicks; }
    public int getTeleportTicks() { return teleportTicks; }
    public void setTeleportTicks(int teleportTicks) { this.teleportTicks = teleportTicks; }
    public int getRespawnExemptTicks() { return respawnExemptTicks; }
    public void setRespawnExemptTicks(int respawnExemptTicks) { this.respawnExemptTicks = respawnExemptTicks; }
    public int getExplosionExemptTicks() { return explosionExemptTicks; }
    public void setExplosionExemptTicks(int explosionExemptTicks) { this.explosionExemptTicks = explosionExemptTicks; }
    public int getVoidTeleportExemptTicks() { return voidTeleportExemptTicks; }
    public void setVoidTeleportExemptTicks(int voidTeleportExemptTicks) { this.voidTeleportExemptTicks = voidTeleportExemptTicks; }
    public double getPendingVelocityX() { return pendingVelocityX; }
    public double getPendingVelocityY() { return pendingVelocityY; }
    public double getPendingVelocityZ() { return pendingVelocityZ; }
    public double getPendingVelocityHorizontal() { return pendingVelocityHorizontal; }
    public long getVelocityReceivedMillis() { return velocityReceivedMillis; }
    public boolean isWaitingForVelocityResponse() { return waitingForVelocityResponse; }
    public void setWaitingForVelocityResponse(boolean waitingForVelocityResponse) { this.waitingForVelocityResponse = waitingForVelocityResponse; }
    public int getLowVelocityStreak() { return lowVelocityStreak; }
    public void setLowVelocityStreak(int lowVelocityStreak) { this.lowVelocityStreak = lowVelocityStreak; }
    public long getBlockBreakStartMillis() { return blockBreakStartMillis; }
    public void setBlockBreakStartMillis(long blockBreakStartMillis) { this.blockBreakStartMillis = blockBreakStartMillis; }
    public Material getBreakingMaterial() { return breakingMaterial; }
    public void setBreakingMaterial(Material breakingMaterial) { this.breakingMaterial = breakingMaterial; }
    public Deque<Long> getScaffoldPlaceTimestamps() { return scaffoldPlaceTimestamps; }
    public Deque<Long> getBlockBreakTimestamps() { return blockBreakTimestamps; }
    public Deque<Long> getChestClickTimestamps() { return chestClickTimestamps; }
    public int getLastChestSlot() { return lastChestSlot; }
    public void setLastChestSlot(int lastChestSlot) { this.lastChestSlot = lastChestSlot; }
}
