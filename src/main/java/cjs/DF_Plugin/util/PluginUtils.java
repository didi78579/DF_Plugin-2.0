package cjs.DF_Plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

public final class PluginUtils {

    private PluginUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * '&' 문자를 사용한 색상 코드를 변환합니다.
     * @param message 색상을 적용할 문자열
     * @return 색상이 적용된 문자열
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Location 객체를 'world,x,y,z' 형태의 문자열로 변환합니다.
     * @param loc 변환할 Location
     * @return 직렬화된 문자열
     */
    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * 'world,x,y,z' 형태의 문자열을 Location 객체로 변환합니다.
     * @param s 직렬화된 위치 문자열
     * @return 변환된 Location 객체, 실패 시 null
     */
    public static Location deserializeLocation(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String[] parts = s.split(",");
        if (parts.length != 4) {
            return null;
        }
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}