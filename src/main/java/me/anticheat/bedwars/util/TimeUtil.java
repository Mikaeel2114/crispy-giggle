package me.anticheat.bedwars.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static long parseDurationMillis(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0L;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("permanent") || normalized.equals("perm") || normalized.equals("-1")) {
            return -1L;
        }
        if (normalized.equals("0") || normalized.equals("0s")) {
            return 0L;
        }
        try {
            if (normalized.endsWith("ms")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2));
            }
            char unit = normalized.charAt(normalized.length() - 1);
            long value = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            switch (unit) {
                case 's':
                    return TimeUnit.SECONDS.toMillis(value);
                case 'm':
                    return TimeUnit.MINUTES.toMillis(value);
                case 'h':
                    return TimeUnit.HOURS.toMillis(value);
                case 'd':
                    return TimeUnit.DAYS.toMillis(value);
                case 'w':
                    return TimeUnit.DAYS.toMillis(value * 7L);
                default:
                    return Long.parseLong(normalized) * 1000L;
            }
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public static String formatDuration(long millis) {
        if (millis < 0L) {
            return "Permanent";
        }
        if (millis == 0L) {
            return "Instant";
        }
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 && days == 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }
}
