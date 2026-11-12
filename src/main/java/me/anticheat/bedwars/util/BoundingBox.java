package me.anticheat.bedwars.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class BoundingBox {

    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static BoundingBox fromPlayerLocation(Location loc) {
        double widthRadius = 0.3D;
        double height = 1.8D;
        return new BoundingBox(
                loc.getX() - widthRadius,
                loc.getY(),
                loc.getZ() - widthRadius,
                loc.getX() + widthRadius,
                loc.getY() + height,
                loc.getZ() + widthRadius
        );
    }

    public static BoundingBox fromEntity(Entity entity) {
        return fromPlayerLocation(entity.getLocation());
    }

    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(
                this.minX - x,
                this.minY - y,
                this.minZ - z,
                this.maxX + x,
                this.maxY + y,
                this.maxZ + z
        );
    }

    public BoundingBox expand(double amount) {
        return expand(amount, amount, amount);
    }

    public Vector getCenter() {
        return new Vector(
                (minX + maxX) * 0.5D,
                (minY + maxY) * 0.5D,
                (minZ + maxZ) * 0.5D
        );
    }

    /**
     * Slab method ray-AABB intersection. Returns the entry distance from origin along normalized direction,
     * or Double.MAX_VALUE if the ray does not intersect the box.
     */
    public double rayTraceDistance(Vector origin, Vector direction, double maxDist) {
        Vector dir = direction.clone().normalize();
        if (contains(origin)) {
            return 0.0D;
        }

        double tMin = 0.0D;
        double tMax = maxDist;

        // X axis
        if (Math.abs(dir.getX()) < 1.0E-7D) {
            if (origin.getX() < minX || origin.getX() > maxX) {
                return Double.MAX_VALUE;
            }
        } else {
            double invD = 1.0D / dir.getX();
            double t1 = (minX - origin.getX()) * invD;
            double t2 = (maxX - origin.getX()) * invD;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.MAX_VALUE;
            }
        }

        // Y axis
        if (Math.abs(dir.getY()) < 1.0E-7D) {
            if (origin.getY() < minY || origin.getY() > maxY) {
                return Double.MAX_VALUE;
            }
        } else {
            double invD = 1.0D / dir.getY();
            double t1 = (minY - origin.getY()) * invD;
            double t2 = (maxY - origin.getY()) * invD;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.MAX_VALUE;
            }
        }

        // Z axis
        if (Math.abs(dir.getZ()) < 1.0E-7D) {
            if (origin.getZ() < minZ || origin.getZ() > maxZ) {
                return Double.MAX_VALUE;
            }
        } else {
            double invD = 1.0D / dir.getZ();
            double t1 = (minZ - origin.getZ()) * invD;
            double t2 = (maxZ - origin.getZ()) * invD;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.MAX_VALUE;
            }
        }

        return tMin >= 0.0D && tMin <= maxDist ? tMin : Double.MAX_VALUE;
    }

    /**
     * Shortest distance from a point (eye position) to the nearest point on the AABB.
     */
    public double distanceToPoint(Vector point) {
        double dx = Math.max(0.0D, Math.max(minX - point.getX(), point.getX() - maxX));
        double dy = Math.max(0.0D, Math.max(minY - point.getY(), point.getY() - maxY));
        double dz = Math.max(0.0D, Math.max(minZ - point.getZ(), point.getZ() - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean contains(Vector point) {
        return point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY
                && point.getZ() >= minZ && point.getZ() <= maxZ;
    }
}
