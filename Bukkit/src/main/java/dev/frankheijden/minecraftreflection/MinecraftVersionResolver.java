package dev.frankheijden.minecraftreflection;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MinecraftVersionResolver {

    private static final Pattern LEGACY_PACKAGE_PATTERN = Pattern.compile("^v?(\\d+)_(\\d+)(?:_R(\\d+))?$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\(MC:\\s*([0-9]+(?:\\.[0-9]+){1,2})\\)");

    private MinecraftVersionResolver() {}

    static VersionResolution resolve(
            String serverPackageName,
            String minecraftVersion,
            String bukkitVersion,
            String serverVersion
    ) {
        Optional<VersionResolution> fromLegacy = parseLegacyPackage(serverPackageName);
        if (fromLegacy.isPresent()) {
            return fromLegacy.get();
        }

        Optional<VersionResolution> fromMinecraftVersion = parseSemanticVersion(minecraftVersion, "minecraft-version");
        if (fromMinecraftVersion.isPresent()) {
            return fromMinecraftVersion.get();
        }

        Optional<VersionResolution> fromBukkitVersion = parseSemanticVersion(bukkitVersion, "bukkit-version");
        if (fromBukkitVersion.isPresent()) {
            return fromBukkitVersion.get();
        }

        Optional<VersionResolution> fromServerVersion = parseServerVersion(serverVersion);
        if (fromServerVersion.isPresent()) {
            return fromServerVersion.get();
        }

        return VersionResolution.unsupported("none", "No parseable version metadata found");
    }

    static Optional<VersionResolution> parseLegacyPackage(String serverPackageName) {
        if (isBlank(serverPackageName)) {
            return Optional.empty();
        }

        int index = serverPackageName.lastIndexOf('.');
        String token = index >= 0 ? serverPackageName.substring(index + 1) : serverPackageName;
        Matcher matcher = LEGACY_PACKAGE_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return Optional.of(VersionResolution.supported("legacy-package", token, major, minor, patch));
    }

    static Optional<VersionResolution> parseSemanticVersion(String input, String source) {
        if (isBlank(input)) {
            return Optional.empty();
        }

        Matcher matcher = SEMVER_PATTERN.matcher(input);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return Optional.of(VersionResolution.supported(source, matcher.group(0), major, minor, patch));
    }

    static Optional<VersionResolution> parseServerVersion(String input) {
        if (isBlank(input)) {
            return Optional.empty();
        }

        Matcher matcher = MC_VERSION_PATTERN.matcher(input);
        if (matcher.find()) {
            return parseSemanticVersion(matcher.group(1), "server-version");
        }
        return parseSemanticVersion(input, "server-version");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class VersionResolution {

        static final int UNSUPPORTED = -1;

        final boolean supported;
        final String source;
        final String rawValue;
        final int major;
        final int minor;
        final int patch;
        final String message;

        private VersionResolution(
                boolean supported,
                String source,
                String rawValue,
                int major,
                int minor,
                int patch,
                String message
        ) {
            this.supported = supported;
            this.source = source;
            this.rawValue = rawValue;
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.message = message;
        }

        static VersionResolution supported(String source, String rawValue, int major, int minor, int patch) {
            return new VersionResolution(true, source, rawValue, major, minor, patch, "");
        }

        static VersionResolution unsupported(String source, String message) {
            return new VersionResolution(
                    false,
                    source,
                    "",
                    UNSUPPORTED,
                    UNSUPPORTED,
                    UNSUPPORTED,
                    message
            );
        }
    }
}
