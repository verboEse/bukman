package net.frankheijden.serverutils.bukkit.entities;

import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import dev.frankheijden.minecraftreflection.MinecraftReflectionVersion;
import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import net.frankheijden.serverutils.bukkit.ServerUtils;
import net.frankheijden.serverutils.bukkit.commands.BukkitCommandPlugins;
import net.frankheijden.serverutils.bukkit.commands.BukkitCommandServerUtils;
import net.frankheijden.serverutils.bukkit.config.BukkitMessageKey;
import net.frankheijden.serverutils.bukkit.listeners.BukkitPlayerListener;
import net.frankheijden.serverutils.bukkit.managers.BukkitPluginManager;
import net.frankheijden.serverutils.bukkit.managers.BukkitTaskManager;
import net.frankheijden.serverutils.common.config.ServerUtilsConfig;
import net.frankheijden.serverutils.common.entities.ServerUtilsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitPlugin extends ServerUtilsPlugin<Plugin, BukkitTask, BukkitAudience, CommandSender, BukkitPluginDescription> {

    private final ServerUtils plugin;
    private final BukkitPluginManager pluginManager;
    private final BukkitTaskManager taskManager;
    private final BukkitResourceProvider resourceProvider;
    private final BukkitAudienceProvider chatProvider;
    private boolean registeredPluginsCommand;

    /**
     * Creates a new BukkitPlugin instance of ServerUtils.
     * @param plugin The ServerUtils plugin.
     */
    public BukkitPlugin(ServerUtils plugin) {
        this.plugin = plugin;
        this.pluginManager = new BukkitPluginManager();
        this.taskManager = new BukkitTaskManager();
        this.resourceProvider = new BukkitResourceProvider(plugin);
        this.chatProvider = new BukkitAudienceProvider(plugin);
        this.registeredPluginsCommand = false;
    }

    @Override
    protected PaperCommandManager<BukkitAudience> newCommandManager() {
        if (getConfigResource() != null && getConfigResource().getConfig().getBoolean("settings.debug-commands")) {
            getLogger().info("[CommandDebug] Creating PaperCommandManager.");
        }
        PaperCommandManager<BukkitAudience> commandManager;
        try {
            commandManager = new PaperCommandManager<>(
                    plugin,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    chatProvider::get,
                    BukkitAudience::getSource
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        boolean hasBrigadier = commandManager.hasCapability(CloudBukkitCapabilities.BRIGADIER);
        boolean hasNativeBrigadier = commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER);
        boolean hasCommodoreBrigadier = commandManager.hasCapability(CloudBukkitCapabilities.COMMODORE_BRIGADIER);
        if (getConfigResource() != null && getConfigResource().getConfig().getBoolean("settings.debug-commands")) {
            getLogger().info("[CommandDebug] Capabilities: brigadier=" + hasBrigadier
                    + ", nativeBrigadier=" + hasNativeBrigadier
                    + ", commodoreBrigadier=" + hasCommodoreBrigadier + ".");
        }
        if (hasBrigadier && (hasNativeBrigadier || hasCommodoreBrigadier)) {
            commandManager.registerBrigadier();
            handleBrigadier(commandManager.brigadierManager());
            if (getConfigResource() != null && getConfigResource().getConfig().getBoolean("settings.debug-commands")) {
                getLogger().info("[CommandDebug] Brigadier bridge registered.");
            }
        }

        if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
            if (getConfigResource() != null && getConfigResource().getConfig().getBoolean("settings.debug-commands")) {
                getLogger().info("[CommandDebug] Asynchronous completions registered.");
            }
        }

        return commandManager;
    }

    @Override
    public Platform getPlatform() {
        return Platform.BUKKIT;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public BukkitPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public BukkitTaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public BukkitResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    @Override
    public BukkitAudienceProvider getChatProvider() {
        return chatProvider;
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    protected void enablePlugin() {
        Bukkit.getPluginManager().registerEvents(new BukkitPlayerListener(this), plugin);
    }

    @Override
    protected void disablePlugin() {
        // nothing to close
    }

    @Override
    protected void reloadPlugin() {
        boolean commandDebugEnabled = getConfigResource().getConfig().getBoolean("settings.debug-commands");
        if (commandDebugEnabled) {
            getLogger().info("[CommandDebug] reloadPlugin started.");
        }
        this.messagesResource.load(Arrays.asList(BukkitMessageKey.values()));
        registerConfiguredPermissions((ServerUtilsConfig) getCommandsResource().getConfig().get("commands"));
        if (!MinecraftReflectionVersion.isSupported()) {
            getLogger().warning(
                    "Skipping reflection-based plugin command rewiring because server version is unsupported."
            );
        } else {
            if (getConfigResource().getConfig().getBoolean("settings.disable-plugins-command")) {
                if (registeredPluginsCommand) {
                    BukkitPluginManager.unregisterCommands("pl", "plugins");
                    plugin.restoreBukkitPluginCommand();
                    this.registeredPluginsCommand = false;
                }
            } else {
                BukkitPluginManager.unregisterCommands("pl", "plugins");
                new BukkitCommandPlugins(this).register(commandManager);
                this.registeredPluginsCommand = true;
                if (commandDebugEnabled) {
                    getLogger().info("[CommandDebug] Registered Bukkit plugins command override.");
                }
            }
        }
        new BukkitCommandServerUtils(this).register(commandManager);
        if (commandDebugEnabled) {
            getLogger().info("[CommandDebug] Registered ServerUtils command tree.");
        }

        taskManager.runTask(() -> BukkitPluginManager.unregisterExactCommands(plugin.getDisabledCommands()));
        if (commandDebugEnabled) {
            getLogger().info("[CommandDebug] Scheduled disabled command cleanup task.");
            getLogger().info("[CommandDebug] reloadPlugin completed.");
        }
    }

    private void registerConfiguredPermissions(ServerUtilsConfig config) {
        if (config == null) {
            return;
        }

        String permission = config.getString("permission");
        if (permission != null
            && !permission.isEmpty()
            && Bukkit.getPluginManager().getPermission(permission) == null) {
            Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.OP));
        }

        for (String key : config.getKeys()) {
            Object value = config.get(key);
            if (value instanceof ServerUtilsConfig) {
                registerConfiguredPermissions((ServerUtilsConfig) value);
            }
        }
    }
}
