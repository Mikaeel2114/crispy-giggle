package me.anticheat.bedwars;

import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.BoundingBox;
import me.anticheat.bedwars.util.TimeUtil;
import org.bukkit.util.Vector;
import org.junit.Assert;
import org.junit.Test;

public class BedwarsACTest {

    @Test
    public void testDurationParsingAndFormatting() {
        Assert.assertEquals(0L, TimeUtil.parseDurationMillis("0"));
        Assert.assertEquals(30L * 60L * 1000L, TimeUtil.parseDurationMillis("30m"));
        Assert.assertEquals(12L * 3600L * 1000L, TimeUtil.parseDurationMillis("12h"));
        Assert.assertEquals(7L * 86400L * 1000L, TimeUtil.parseDurationMillis("7d"));
        Assert.assertEquals(30L * 86400L * 1000L, TimeUtil.parseDurationMillis("30d"));
        Assert.assertEquals(90L * 86400L * 1000L, TimeUtil.parseDurationMillis("90d"));
        Assert.assertEquals(-1L, TimeUtil.parseDurationMillis("permanent"));

        Assert.assertEquals("Permanent", TimeUtil.formatDuration(-1L));
        Assert.assertEquals("Instant", TimeUtil.formatDuration(0L));
        Assert.assertTrue(TimeUtil.formatDuration(86400000L).contains("1d"));
    }

    @Test
    public void testBoundingBoxRayTraceAndDistance() {
        BoundingBox box = new BoundingBox(-0.3D, 0.0D, 2.7D, 0.3D, 1.8D, 3.3D).expand(0.1D);
        Vector eye = new Vector(0.0D, 1.62D, 0.0D);
        Vector forward = new Vector(0.0D, 0.0D, 1.0D);
        Vector backward = new Vector(0.0D, 0.0D, -1.0D);

        double hitDist = box.rayTraceDistance(eye, forward, 6.0D);
        Assert.assertEquals(2.6D, hitDist, 0.02D);

        double missDist = box.rayTraceDistance(eye, backward, 6.0D);
        Assert.assertEquals(Double.MAX_VALUE, missDist, 0.001D);

        double pointDist = box.distanceToPoint(eye);
        Assert.assertEquals(2.6D, pointDist, 0.02D);
    }

    @Test
    public void testAngleWrapping() {
        Assert.assertEquals(0.0F, PlayerData.wrapAngle180(360.0F), 0.001F);
        Assert.assertEquals(-170.0F, PlayerData.wrapAngle180(190.0F), 0.001F);
        Assert.assertEquals(170.0F, PlayerData.wrapAngle180(-190.0F), 0.001F);
    }
}
