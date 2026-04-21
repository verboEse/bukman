package dev.frankheijden.minecraftreflection;

import org.bukkit.Bukkit;

/**
 * Resilient server version resolver used by reflection helpers.
 *
 * <p>
 * Metadata sources (in order):
 * 1) Legacy package token from server package name (for example v1_16_R3)
 * 2) Bukkit-provided minecraft version string
 * 3) Bukkit version string
 * 4) Full server version string (MC: x.y.z)
 */
public class MinecraftReflectionVersion {

    public static final String NMS;
    public static final int MAJOR;
    public static final int MINOR;
    public static final int PATCH;

    private static final boolean SUPPORTED;
    private static final String DETECTION_SOURCE;
    private static final String DETECTED_VALUE;

    static {
        String packageName = "";
        String minecraftVersion = "";
        String bukkitVersion = "";
        String serverVersion = "";

        try {
            packageName = Bukkit.getServer().getClass().getPackage().getName();
        } catch (Throwable ignored) {
            // Keep blank; resolver will try other sources.
        }

        try {
            bukkitVersion = Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {
            // Keep blank; resolver will try other sources.
        }

        try {
            serverVersion = Bukkit.getVersion();
        } catch (Throwable ignored) {
            // Keep blank; resolver will try other sources.
        }

        try {
            Object result = Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
            if (result != null) {
                minecraftVersion = result.toString();
            }
        } catch (Throwable ignored) {
            // Method availability differs by API version.
        }

        MinecraftVersionResolver.VersionResolution resolved = MinecraftVersionResolver.resolve(
                packageName,
                minecraftVersion,
                bukkitVersion,
                serverVersion
        );

        SUPPORTED = resolved.supported;
        DETECTION_SOURCE = resolved.source;
        DETECTED_VALUE = resolved.rawValue;

        MAJOR = resolved.major;
        MINOR = resolved.minor;
        PATCH = resolved.patch;
        NMS = extractNmsToken(packageName);

        if (!SUPPORTED) {
            Bukkit.getLogger().warning("[Bukman] Unable to resolve supported Minecraft server version. "
                    + "Reflection-dependent features will be disabled. "
                    + "source=" + DETECTION_SOURCE + ", package='" + packageName + "', "
                    + "minecraftVersion='" + minecraftVersion + "', bukkitVersion='" + bukkitVersion + "', "
                    + "serverVersion='" + serverVersion + "'.");
        }
    }

    private static String extractNmsToken(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        int index = packageName.lastIndexOf('.');
        return index >= 0 ? packageName.substring(index + 1) : packageName;
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Returns where version metadata was detected from.
     */
    public static String getDetectionSource() {
        return DETECTION_SOURCE;
    }

    /**
     * Returns the raw version value used during detection.
     */
    public static String getDetectedValue() {
        return DETECTED_VALUE;
    }

    /**
     * Checks whether the detected minor version equals the given major argument.
     */
    public static boolean is(int major) {
        return SUPPORTED && MINOR == major;
    }

    /**
     * Checks whether detected minor and patch versions match exactly.
     */
    public static boolean is(int major, int patch) {
        return SUPPORTED && MINOR == major && PATCH == patch;
    }

    /**
     * Checks whether detected minor version is at least the given major argument.
     */
    public static boolean isMin(int major) {
        return SUPPORTED && MINOR >= major;
    }

    /**
     * Checks whether detected minor+patch is at least the given target.
     */
    public static boolean isMin(int major, int patch) {
        if (!SUPPORTED) {
            return false;
        }
        return MINOR > major || (MINOR == major && PATCH >= patch);
    }

    /**
     * Checks whether detected minor version is at most the given major argument.
     */
    public static boolean isMax(int major) {
        return SUPPORTED && MINOR <= major;
    }

    /**
     * Checks whether detected minor+patch is at most the given target.
     */
    public static boolean isMax(int major, int patch) {
        if (!SUPPORTED) {
            return false;
        }
        return MINOR < major || (MINOR == major && PATCH <= patch);
    }
}
