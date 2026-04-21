<!-- Variables (this block will not be visible in the readme -->
[gradleInstall]: https://gradle.org/install/
<!-- End of variables block -->

# Bukman
Bukman allows you to manage your plugins in-game.
Featuring reloading, unloading and loading of plugins from your plugins folder at runtime.
Bukman also has handy methods to lookup commands and plugins,
and provides you with handy information about them.

## Compiling Bukman
There are two ways to compile Bukman:
### 1. Installing gradle (recommended)
1. Make sure you have [gradle][gradleInstall] installed.
2. Run the project with `gradle build` to compile it with dependencies.
### 2. Using the wrapper
**Windows**: `gradlew.bat build`
<br>
**Linux/macOS**: `./gradlew build`

## Local testing on latest Purpur experimental

1. Prepare a local test server (downloads latest Purpur experimental for 26.1.2 and installs Bukman):
  - `./scripts/purpur-experimental-setup.sh`
2. Start the server:
  - `./scripts/purpur-experimental-run.sh`

By default, the test server is created at `.testserver/purpur-experimental`.
You can override this location with `BUKMAN_TEST_SERVER_DIR=/path/to/server`.

The setup script enables command tracing automatically in `plugins/Bukman/config.yml`:

```yml
settings:
  debug-commands: true
```

When enabled, Bukman writes command flow markers to the server log prefixed with `[CommandDebug]`.

#### Gradle:
```kotlin
repositories { 
  maven("https://maven.zhira.net/repository/zhdev/")
}

dependencies {
  compileOnly("org.zhdev.bukman:Bukman:VERSION")
}
```

#### Maven:
```xml
<project>
  <repositories>
    <!-- Bukman repo -->
    <repository>
      <id>zhdev-repo</id>
      <url>https://maven.zhira.net/repository/zhdev/</url>
    </repository>
  </repositories>
  
  <dependencies>
    <!-- Bukman dependency -->
    <dependency>
      <groupId>org.zhdev.bukman</groupId>
      <artifactId>Bukman</artifactId>
      <version>VERSION</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</project>
```
