package dev.frankheijden.minecraftreflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MinecraftVersionResolverTest {

    @Test
    void parsesLegacyPackageToken() {
        Optional<MinecraftVersionResolver.VersionResolution> resolution = MinecraftVersionResolver.parseLegacyPackage(
                "org.bukkit.craftbukkit.v1_16_R3"
        );

        assertThat(resolution).isPresent();
        assertThat(resolution.get().supported).isTrue();
        assertThat(resolution.get().major).isEqualTo(1);
        assertThat(resolution.get().minor).isEqualTo(16);
        assertThat(resolution.get().patch).isEqualTo(3);
        assertThat(resolution.get().source).isEqualTo("legacy-package");
    }

    @Test
    void ignoresInvalidLegacyToken() {
        Optional<MinecraftVersionResolver.VersionResolution> resolution = MinecraftVersionResolver.parseLegacyPackage(
                "org.bukkit.craftbukkit"
        );

        assertThat(resolution).isEmpty();
    }

    @Test
    void resolvesFromBukkitVersionWhenLegacyTokenInvalid() {
        MinecraftVersionResolver.VersionResolution resolution = MinecraftVersionResolver.resolve(
                "org.bukkit.craftbukkit",
                "",
                "1.21.5-R0.1-SNAPSHOT",
                ""
        );

        assertThat(resolution.supported).isTrue();
        assertThat(resolution.major).isEqualTo(1);
        assertThat(resolution.minor).isEqualTo(21);
        assertThat(resolution.patch).isEqualTo(5);
        assertThat(resolution.source).isEqualTo("bukkit-version");
    }

    @Test
    void fallsBackToUnsupportedWhenNoSourceMatches() {
        MinecraftVersionResolver.VersionResolution resolution = MinecraftVersionResolver.resolve(
                "org.bukkit.craftbukkit",
                "unknown",
                "snapshot",
                "development-build"
        );

        assertThat(resolution.supported).isFalse();
        assertThat(resolution.minor).isEqualTo(MinecraftVersionResolver.VersionResolution.UNSUPPORTED);
    }

    @Test
    void preservesLegacyPriorityOverModernVersionStrings() {
        MinecraftVersionResolver.VersionResolution resolution = MinecraftVersionResolver.resolve(
                "org.bukkit.craftbukkit.v1_16_R3",
                "1.16.5",
                "1.16.5-R0.1-SNAPSHOT",
                "git-Paper-123 (MC: 1.16.5)"
        );

        assertThat(resolution.supported).isTrue();
        assertThat(resolution.minor).isEqualTo(16);
        assertThat(resolution.patch).isEqualTo(3);
        assertThat(resolution.source).isEqualTo("legacy-package");
    }
}
