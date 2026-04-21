package net.frankheijden.serverutils.bukkit.reflection;

import dev.frankheijden.minecraftreflection.MinecraftReflection;
import dev.frankheijden.minecraftreflection.MinecraftReflectionVersion;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class RCraftServer {

    private static volatile MinecraftReflection reflection;

    /**
     * Returns the CraftServer reflection helper, initializing it on first use.
     */
    public static MinecraftReflection getReflection() {
        MinecraftReflection cached = reflection;
        if (cached != null) {
            return cached;
        }

        synchronized (RCraftServer.class) {
            cached = reflection;
            if (cached == null) {
                Object server = Bukkit.getServer();
                if (server == null) {
                    throw new IllegalStateException("Bukkit server is not available yet");
                }
                cached = MinecraftReflection.of(server.getClass());
                reflection = cached;
            }
        }

        return cached;
    }

    public static File getConfigFile() {
        return getReflection().invoke(Bukkit.getServer(), "getConfigFile");
    }

    /**
     * Retrieves the options file from a key.
     * @param option The option key.
     * @return The associated file.
     */
    public static File getOptionsFile(String option) {
        Object options = getReflection().get(getConsole(), "options");
        return getReflection().invoke(options, "valueOf", option);
    }

    public static File getCommandsConfigFile() {
        return getReflection().invoke(Bukkit.getServer(), "getCommandsConfigFile");
    }

    public static SimpleCommandMap getCommandMap() {
        return getReflection().get(Bukkit.getServer(), "commandMap");
    }

    /**
     * Syncs and registers all commands, but keeping the old values that haven't been added.
     */
    @SuppressWarnings({"rawtypes"})
    public static void syncCommands(Set<String> removedCommands) {
        if (MinecraftReflectionVersion.MINOR < 13) return;

        Collection children = new ArrayList<>(RCommandDispatcher.getDispatcher().getRoot().getChildren());
        getReflection().invoke(Bukkit.getServer(), "syncCommands");
        Object root = RCommandDispatcher.getDispatcher().getRoot();

        for (Object child : children) {
            String name = RCommandNode.getName(child);
            RCommandNode.removeCommand(root, name);

            if (!removedCommands.contains(name)) {
                RCommandNode.addChild(root, child);
            }
        }
        updateCommands();
    }

    /**
     * Updates commands for all online players.
     */
    public static void updateCommands() {
        if (MinecraftReflectionVersion.MINOR < 13) return;
        Bukkit.getOnlinePlayers().forEach(RCraftServer::updateCommands);
    }

    public static void updateCommands(Player player) {
        if (MinecraftReflectionVersion.MINOR < 13) return;
        player.updateCommands();
    }

    public static Object getConsole() {
        return getReflection().get(Bukkit.getServer(), "console");
    }

    /**
     * Reloads the bukkit configuration.
     */
    public static void reloadBukkitConfiguration() {
        YamlConfiguration bukkit = YamlConfiguration.loadConfiguration(getConfigFile());
        getReflection().set(Bukkit.getServer(), "configuration", bukkit);

        RDedicatedServer.reload(getConsole());

        getReflection().set(Bukkit.getServer(), "monsterSpawn", bukkit.getInt("spawn-limits.monsters"));
        getReflection().set(Bukkit.getServer(), "animalSpawn", bukkit.getInt("spawn-limits.animals"));
        getReflection().set(Bukkit.getServer(), "waterAnimalSpawn", bukkit.getInt("spawn-limits.water-animals"));
        getReflection().set(Bukkit.getServer(), "ambientSpawn", bukkit.getInt("spawn-limits.ambient"));
        getReflection().set(Bukkit.getServer(), "warningState",
                Warning.WarningState.value(bukkit.getString("settings.deprecated-verbose")));
        if (MinecraftReflectionVersion.isMin(14))
            getReflection().set(Bukkit.getServer(), "minimumAPI", bukkit.getString("settings.minimum-api"));
        getReflection().set(Bukkit.getServer(), "printSaveWarning", false);
        if (MinecraftReflectionVersion.isMax(12)) {
            getReflection().set(Bukkit.getServer(), "chunkGCPeriod", bukkit.getInt("chunk-gc.period-in-ticks"));
            getReflection().set(Bukkit.getServer(), "chunkGCLoadThresh", bukkit.getInt("chunk-gc.load-threshold"));
        }

        RDedicatedServer.getReflection().set(getConsole(), "autosavePeriod", bukkit.getInt("ticks-per.autosave"));
    }

    public static void loadIcon() {
        getReflection().invoke(Bukkit.getServer(), "loadIcon");
    }

    /**
     * Reloads the commands.yml file.
     */
    public static void reloadCommandsConfiguration() {
        SimpleCommandMap commandMap = getCommandMap();
        Map<String, Command> map = RCommandMap.getKnownCommands(commandMap);

        Set<String> commandNames = Bukkit.getCommandAliases().keySet();
        RCommandDispatcher.removeCommands(commandNames);
        for (String alias : commandNames) {
            Command aliasCommand = map.remove(alias);
            if (aliasCommand == null) continue;

            aliasCommand.unregister(commandMap);
        }

        YamlConfiguration commands = YamlConfiguration.loadConfiguration(getCommandsConfigFile());
        getReflection().set(Bukkit.getServer(), "commandsConfiguration", commands);
        getReflection().set(Bukkit.getServer(), "overrideAllCommandBlockCommands",
                commands.getStringList("command-block-overrides").contains("*"));
        if (MinecraftReflectionVersion.isMin(13)) getReflection().set(
                Bukkit.getServer(),
                "ignoreVanillaPermissions",
                commands.getBoolean("ignore-vanilla-permissions")
        );
        if (MinecraftReflectionVersion.is(12)) getReflection().set(
                Bukkit.getServer(),
                "unrestrictedAdvancements",
                commands.getBoolean("unrestricted-advancements")
        );

        commandMap.registerServerAliases();
        RCraftServer.syncCommands(commandNames);
    }

    /**
     * Reloads the ip-bans file.
     */
    public static void reloadIpBans() {
        Object playerList = getReflection().get(Bukkit.getServer(), "playerList");
        Object jsonList = RPlayerList.getReflection().invoke(playerList, "getIPBans");
        RJsonList.load(jsonList);
    }

    /**
     * Reloads the profile bans file.
     */
    public static void reloadProfileBans() {
        Object playerList = getReflection().get(Bukkit.getServer(), "playerList");
        Object jsonList = RPlayerList.getReflection().invoke(playerList, "getProfileBans");
        RJsonList.load(jsonList);
    }
}
