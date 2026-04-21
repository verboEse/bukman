package net.frankheijden.serverutils.common.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.frankheijden.serverutils.common.commands.arguments.JarFilesArgument;
import net.frankheijden.serverutils.common.commands.arguments.PluginArgument;
import net.frankheijden.serverutils.common.commands.arguments.PluginsArgument;
import net.frankheijden.serverutils.common.config.MessageKey;
import net.frankheijden.serverutils.common.config.MessagesResource;
import net.frankheijden.serverutils.common.config.ServerUtilsConfig;
import net.frankheijden.serverutils.common.entities.results.CloseablePluginResults;
import net.frankheijden.serverutils.common.entities.results.PluginResult;
import net.frankheijden.serverutils.common.entities.results.PluginResults;
import net.frankheijden.serverutils.common.entities.results.PluginWatchResults;
import net.frankheijden.serverutils.common.entities.results.Result;
import net.frankheijden.serverutils.common.entities.ServerUtilsAudience;
import net.frankheijden.serverutils.common.entities.ServerUtilsPlugin;
import net.frankheijden.serverutils.common.managers.AbstractPluginManager;
import net.frankheijden.serverutils.common.tasks.UpdateCheckerTask;
import net.frankheijden.serverutils.common.utils.ListComponentBuilder;
import net.frankheijden.serverutils.common.utils.KeyValueComponentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public abstract class CommandServerUtils<U extends ServerUtilsPlugin<P, ?, C, ?, ?>, P, C extends ServerUtilsAudience<?>>
        extends ServerUtilsCommand<U, C> {

    protected final IntFunction<P[]> arrayCreator;

    protected CommandServerUtils(U plugin, IntFunction<P[]> arrayCreator) {
        super(plugin, "serverutils");
        this.arrayCreator = arrayCreator;
    }

    @Override
    public void register(CommandManager<C> manager, Command.Builder<C> builder) {
        debugCommand("Preparing arguments and subcommands for '" + commandName + "'.");
        addArgument(new JarFilesArgument<>(true, "jarFiles", plugin));
        addArgument(new PluginsArgument<>(true, "plugins", new PluginsArgument.PluginsParser<>(plugin, arrayCreator)));
        addArgument(new PluginArgument<>(true, "plugin", plugin));
        addArgument(CommandArgument.<C, String>ofType(String.class, "command")
                .manager(manager)
                .withSuggestionsProvider((context, s) -> new ArrayList<>(plugin.getPluginManager().getCommands()))
                .build());

        manager.command(builder
                .handler(this::handleHelpCommand));
        registerSubcommand(manager, builder, "help", subcommandBuilder -> subcommandBuilder
                .handler(this::handleHelpCommand));
        registerSubcommand(manager, builder, "reload", subcommandBuilder -> subcommandBuilder
                .handler(this::handleReload));
        registerSubcommand(manager, builder, "restart", subcommandBuilder -> subcommandBuilder
                .handler(this::handleRestart));
        registerSubcommand(manager, builder, "loadplugin", subcommandBuilder -> subcommandBuilder
                .argument(getArgument("jarFiles"))
                .handler(this::handleLoadPlugin));
        registerSubcommand(manager, builder, "unloadplugin", subcommandBuilder -> subcommandBuilder
                .argument(new PluginsArgument<>(
                        true,
                        "plugins",
                        new PluginsArgument.PluginsParser<>(plugin, arrayCreator, getRawPath("unloadplugin"))
                ))
                .handler(this::handleUnloadPlugin));
        registerSubcommand(manager, builder, "reloadplugin", subcommandBuilder -> subcommandBuilder
                .argument(new PluginsArgument<>(
                        true,
                        "plugins",
                        new PluginsArgument.PluginsParser<>(plugin, arrayCreator, getRawPath("reloadplugin"))
                ))
                .handler(this::handleReloadPlugin));
        registerSubcommand(manager, builder, "watchplugin", subcommandBuilder -> subcommandBuilder
                .argument(new PluginsArgument<>(
                        true,
                        "plugins",
                        new PluginsArgument.PluginsParser<>(plugin, arrayCreator, getRawPath("watchplugin"))
                ))
                .handler(this::handleWatchPlugin));
        registerSubcommand(manager, builder, "unwatchplugin", subcommandBuilder -> subcommandBuilder
                .argument(getArgument("plugin"))
                .handler(this::handleUnwatchPlugin));
        registerSubcommand(manager, builder, "plugininfo", subcommandBuilder -> subcommandBuilder
                .argument(getArgument("plugin"))
                .handler(this::handlePluginInfo));
        registerSubcommand(manager, builder, "commandinfo", subcommandBuilder -> subcommandBuilder
                .argument(getArgument("command"))
                .handler(this::handleCommandInfo));
        debugCommand("Finished wiring handlers for '" + commandName + "'.");
    }

    private void handleHelpCommand(CommandContext<C> context) {
        debugCommand("handleHelpCommand started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();

        MessagesResource messages = plugin.getMessagesResource();
        sender.sendMessage(messages.get(MessageKey.HELP_HEADER).toComponent());

        MessagesResource.Message helpFormatMessage = messages.get(MessageKey.HELP_FORMAT);

        ServerUtilsConfig config = (ServerUtilsConfig) plugin.getCommandsResource().getConfig().get("commands");
        for (String commandName : config.getKeys()) {
            ServerUtilsConfig commandConfig = (ServerUtilsConfig) config.get(commandName);
            CommandElement commandElement = parseElement(commandConfig);
            String shortestCommandAlias = determineShortestAlias(commandElement);

            if (commandElement.shouldDisplayInHelp()) {
                sender.sendMessage(helpFormatMessage.toComponent(
                        TagResolver.resolver("command", Tag.inserting(Component.text(shortestCommandAlias))),
                        TagResolver.resolver("help",
                                Tag.inserting(Component.text(commandElement.getDescription().getDescription())))
                ));
            }

            Object subcommandsObject = commandConfig.get("subcommands");
            if (subcommandsObject instanceof ServerUtilsConfig) {
                ServerUtilsConfig subcommandsConfig = (ServerUtilsConfig) subcommandsObject;

                for (String subcommandName : subcommandsConfig.getKeys()) {
                    ServerUtilsConfig subcommandConfig = (ServerUtilsConfig) subcommandsConfig.get(subcommandName);
                    CommandElement subcommandElement = parseElement(subcommandConfig);
                    if (subcommandElement.shouldDisplayInHelp()) {
                        String shortestSubcommandAlias = determineShortestAlias(subcommandElement);
                        sender.sendMessage(helpFormatMessage.toComponent(
                                TagResolver.resolver("command",
                                        Tag.inserting(Component.text(shortestCommandAlias + ' '
                                                + shortestSubcommandAlias))),
                                TagResolver.resolver("help",
                                        Tag.inserting(Component.text(subcommandElement.getDescription()
                                                .getDescription())))
                        ));
                    }
                }
            }

            Object flagsObject = commandConfig.get("flags");
            if (flagsObject instanceof ServerUtilsConfig) {
                ServerUtilsConfig flagsConfig = (ServerUtilsConfig) flagsObject;

                for (String flagName : flagsConfig.getKeys()) {
                    ServerUtilsConfig flagConfig = (ServerUtilsConfig) flagsConfig.get(flagName);
                    CommandElement flagElement = parseElement(flagConfig);
                    if (flagElement.shouldDisplayInHelp()) {
                        String shortestFlagAlias = determineShortestAlias(flagElement);
                        String flagPrefix = "-" + (flagElement.getMain().equals(shortestFlagAlias) ? "_" : "");
                        sender.sendMessage(helpFormatMessage.toComponent(
                                TagResolver.resolver("command",
                                        Tag.inserting(Component.text(shortestCommandAlias + ' ' + flagPrefix
                                                + shortestFlagAlias))),
                                TagResolver.resolver("help",
                                        Tag.inserting(Component.text(flagElement.getDescription().getDescription())))
                        ));
                    }
                }
            }
        }

        sender.sendMessage(messages.get(MessageKey.HELP_FOOTER).toComponent());
        debugCommand("handleHelpCommand completed.");
    }

    private String determineShortestAlias(CommandElement element) {
        String shortestAlias = element.getMain();
        for (String alias : element.getAliases()) {
            if (alias.length() < shortestAlias.length()) {
                shortestAlias = alias;
            }
        }
        return shortestAlias;
    }

    private void handleReload(CommandContext<C> context) {
        debugCommand("handleReload started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        plugin.reload();
        plugin.getMessagesResource().get(MessageKey.RELOAD).sendTo(sender);
        debugCommand("handleReload completed.");
    }

    private void handleRestart(CommandContext<C> context) {
        debugCommand("handleRestart started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();

        if (checkDependingPlugins(context, sender, Collections.singletonList(plugin.getPlugin()), "restart")) {
            debugCommand("handleRestart stopped: depending plugins blocked restart.");
            return;
        }

        UpdateCheckerTask.restart(sender);
        debugCommand("handleRestart handed off to UpdateCheckerTask.restart.");
    }

    private void handleLoadPlugin(CommandContext<C> context) {
        debugCommand("handleLoadPlugin started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        List<File> jarFiles = Arrays.asList(context.get("jarFiles"));
        debugCommand("handleLoadPlugin loading " + jarFiles.size() + " jar file(s).");

        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        PluginResults<P> loadResults = pluginManager.loadPlugins(jarFiles);
        if (!loadResults.isSuccess()) {
            debugCommand("handleLoadPlugin failed during loadPlugins.");
            PluginResult<P> failedResult = loadResults.last();
            failedResult.sendTo(sender, null);
            return;
        }

        PluginResults<P> enableResults = pluginManager.enablePlugins(loadResults.getPlugins());
        enableResults.sendTo(sender, MessageKey.LOADPLUGIN);
        debugCommand("handleLoadPlugin completed.");
    }

    private void handleUnloadPlugin(CommandContext<C> context) {
        debugCommand("handleUnloadPlugin started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        List<P> plugins = new ArrayList<>(Arrays.asList(context.get("plugins")));

        if (checkProtectedPlugins(sender, plugins)) {
            debugCommand("handleUnloadPlugin stopped: protected plugin in selection.");
            return;
        }

        boolean recursive = context.flags().contains("recursive");

        Collection<P> dependingPlugins = getDependingPlugins(context, sender, plugins, "unloadplugin");
        if (!recursive) {
            if (!dependingPlugins.isEmpty()) {
                debugCommand("handleUnloadPlugin stopped: depending plugins found without recursive flag.");
                return;
            }
        } else {
            plugins.addAll(0, dependingPlugins);
        }

        PluginResults<P> disableResults = plugin.getPluginManager().disablePlugins(plugins);
        for (PluginResult<P> disableResult : disableResults.getResults()) {
            if (!disableResult.isSuccess() && disableResult.getResult() != Result.ALREADY_DISABLED) {
                debugCommand("handleUnloadPlugin failed while disabling plugins.");
                disableResult.sendTo(sender, null);
                return;
            }
        }

        CloseablePluginResults<P> unloadResults = plugin.getPluginManager().unloadPlugins(plugins);
        unloadResults.tryClose();
        unloadResults.sendTo(sender, MessageKey.UNLOADPLUGIN);
        debugCommand("handleUnloadPlugin completed.");
    }

    private void handleReloadPlugin(CommandContext<C> context) {
        debugCommand("handleReloadPlugin started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        List<P> plugins = Arrays.asList(context.get("plugins"));

        if (checkProtectedPlugins(sender, plugins)) {
            debugCommand("handleReloadPlugin stopped: protected plugin in selection.");
            return;
        }

        boolean recursive = context.flags().contains("recursive");

        Collection<P> dependingPlugins = getDependingPlugins(context, sender, plugins, "reloadplugin");
        if (!recursive && !dependingPlugins.isEmpty()) {
            debugCommand("handleReloadPlugin stopped: depending plugins found without recursive flag.");
            return;
        }

        if (checkServerUtils(context, sender, plugins)) {
            debugCommand("handleReloadPlugin stopped: target includes ServerUtils plugin.");
            return;
        }

        if (recursive && !dependingPlugins.isEmpty()) {
            PluginResults<P> disableDependingResults = plugin.getPluginManager().disablePlugins(dependingPlugins);
            for (PluginResult<P> disableResult : disableDependingResults.getResults()) {
                if (!disableResult.isSuccess() && disableResult.getResult() != Result.ALREADY_DISABLED) {
                    debugCommand("handleReloadPlugin failed while disabling depending plugins.");
                    disableResult.sendTo(sender, null);
                    return;
                }
            }

            CloseablePluginResults<P> unloadDependingResults = plugin.getPluginManager()
                    .unloadPlugins(dependingPlugins);
            unloadDependingResults.tryClose();
            unloadDependingResults.sendTo(sender, MessageKey.UNLOADPLUGIN_RECURSIVELY);
        }

        PluginResults<P> reloadResults = plugin.getPluginManager().reloadPlugins(plugins);
        reloadResults.sendTo(sender, MessageKey.RELOADPLUGIN_SUCCESS);

        if (recursive && !dependingPlugins.isEmpty()) {
            List<String> pluginIds = new ArrayList<>(dependingPlugins.size());
            for (P p : dependingPlugins) {
                pluginIds.add(plugin.getPluginManager().getPluginId(p));
            }
            List<File> pluginFiles = new ArrayList<>(dependingPlugins.size());
            for (String pluginId : pluginIds) {
                Optional<File> pluginFile = plugin.getPluginManager().getPluginFile(pluginId);
                if (!pluginFile.isPresent()) {
                    new PluginResults<P>().addResult(pluginId, Result.FILE_DELETED).sendTo(sender, null);
                    continue;
                }
                pluginFiles.add(pluginFile.get());
            }

            PluginResults<P> loadDependingResults = plugin.getPluginManager().loadPlugins(pluginFiles);
            if (!loadDependingResults.isSuccess()) {
                debugCommand("handleReloadPlugin failed while loading depending plugins.");
                PluginResult<P> failedResult = loadDependingResults.last();
                failedResult.sendTo(sender, null);
            }

            PluginResults<P> enableResults = plugin.getPluginManager().enablePlugins(loadDependingResults.getPlugins());
            enableResults.sendTo(sender, MessageKey.LOADPLUGIN_RECURSIVELY);
        }
        debugCommand("handleReloadPlugin completed.");
    }

    protected boolean checkDependingPlugins(CommandContext<C> context, C sender, List<P> plugins, String subcommand) {
        return !getDependingPlugins(context, sender, plugins, subcommand).isEmpty();
    }

    protected Collection<P> getDependingPlugins(CommandContext<C> context, C sender, List<P> plugins,
            String subcommand) {
        if (context.flags().contains("force")) {
            debugCommand("getDependingPlugins bypassed by force flag for subcommand '" + subcommand + "'.");
            return Collections.emptySet();
        }

        boolean recursive = context.flags().contains("recursive");

        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        MessagesResource messages = plugin.getMessagesResource();

        Map<String, P> dependingPluginsAll = new HashMap<>();
        boolean hasDependingPlugins = false;
        for (P plugin : plugins) {
            String pluginId = pluginManager.getPluginId(plugin);

            List<P> dependingPlugins = pluginManager.getPluginsDependingOn(pluginId);
            if (!dependingPlugins.isEmpty()) {
                if (!recursive) {
                    TextComponent.Builder builder = Component.text();
                    builder.append(messages.get(MessageKey.DEPENDING_PLUGINS_PREFIX).toComponent(
                            TagResolver.resolver("plugin", Tag.inserting(Component.text(pluginId)))
                    ));
                    builder.append(ListComponentBuilder.create(dependingPlugins)
                            .format(p -> messages.get(MessageKey.DEPENDING_PLUGINS_FORMAT).toComponent(
                                    TagResolver.resolver("plugin",
                                            Tag.inserting(Component.text(pluginManager.getPluginId(p))))
                            ))
                            .separator(messages.get(MessageKey.DEPENDING_PLUGINS_SEPARATOR).toComponent())
                            .lastSeparator(messages.get(MessageKey.DEPENDING_PLUGINS_LAST_SEPARATOR).toComponent())
                            .build());
                    sender.sendMessage(builder.build());
                }

                hasDependingPlugins = true;
                for (P dependingPlugin : dependingPlugins) {
                    dependingPluginsAll.putIfAbsent(pluginManager.getPluginId(dependingPlugin), dependingPlugin);
                }
            }
        }

        if (!recursive && hasDependingPlugins) {
            String forceFlagPath = getRawPath(subcommand) + ".flags.force";
            String forceFlag = plugin.getCommandsResource().getAllFlagAliases(forceFlagPath).stream()
                    .min(Comparator.comparingInt(String::length))
                    .orElse("-f");
            String recursiveFlagPath = getRawPath(subcommand) + ".flags.recursive";
            String rescuriveFlag = plugin.getCommandsResource().getAllFlagAliases(recursiveFlagPath).stream()
                    .min(Comparator.comparingInt(String::length))
                    .orElse("-r");

            sender.sendMessage(messages.get(MessageKey.DEPENDING_PLUGINS_OVERRIDE).toComponent(
                    TagResolver.resolver("command", Tag.inserting(Component.text(context.getRawInputJoined() + " "
                            + forceFlag)))
            ));
            sender.sendMessage(messages.get(MessageKey.DEPENDING_PLUGINS_RECURSIVELY).toComponent(
                    TagResolver.resolver("command", Tag.inserting(Component.text(context.getRawInputJoined() + " "
                            + rescuriveFlag)))
            ));
        }

        if (!dependingPluginsAll.isEmpty()) {
            String debugMessage = "getDependingPlugins found " + dependingPluginsAll.size()
                    + " dependent plugin(s) for subcommand '" + subcommand + "'.";
            debugCommand(debugMessage);
        }
        return dependingPluginsAll.values();
    }

    protected boolean checkServerUtils(CommandContext<C> context, C sender, List<P> plugins) {
        for (P loadedPlugin : plugins) {
            if (plugin.getPlugin() == loadedPlugin) {
                String debugMessage = "checkServerUtils matched self plugin for raw input '"
                        + context.getRawInputJoined() + "'.";
                debugCommand(debugMessage);
                String restartCommand = plugin.getCommandsResource().getAllAliases(getRawPath("restart")).stream()
                        .min(Comparator.comparingInt(String::length))
                        .orElse("restart");
                Component component = plugin.getMessagesResource().get(MessageKey.RELOADPLUGIN_SERVERUTILS)
                        .toComponent(TagResolver.resolver("command",
                                Tag.inserting(Component.text(context.getRawInput().peekFirst() + " "
                                        + restartCommand)))
                );
                sender.sendMessage(component);
                return true;
            }
        }

        return false;
    }

    protected boolean checkProtectedPlugins(C sender, List<P> plugins) {
        List<String> protectedPlugins = plugin.getConfigResource().getConfig().getStringList("protected-plugins");
        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        MessagesResource messagesResource = plugin.getMessagesResource();
        for (P plugin : plugins) {
            String pluginId = pluginManager.getPluginId(plugin);
            if (protectedPlugins.contains(pluginId)) {
                debugCommand("checkProtectedPlugins blocked plugin '" + pluginId + "'.");
                sender.sendMessage(messagesResource.get(MessageKey.GENERIC_PROTECTED_PLUGIN).toComponent(
                        TagResolver.resolver("plugin", Tag.inserting(Component.text(pluginId)))
                ));
                return true;
            }
        }
        return false;
    }

    private void handleWatchPlugin(CommandContext<C> context) {
        debugCommand("handleWatchPlugin started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        List<P> plugins = Arrays.asList(context.get("plugins"));

        if (checkDependingPlugins(context, sender, plugins, "watchplugin")) {
            debugCommand("handleWatchPlugin stopped: depending plugins blocked watch.");
            return;
        }

        if (checkServerUtils(context, sender, plugins)) {
            debugCommand("handleWatchPlugin stopped: target includes ServerUtils plugin.");
            return;
        }

        PluginWatchResults watchResults = plugin.getWatchManager().watchPlugins(sender, plugins);
        watchResults.sendTo(sender);
        debugCommand("handleWatchPlugin completed.");
    }

    private void handleUnwatchPlugin(CommandContext<C> context) {
        debugCommand("handleUnwatchPlugin started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        P pluginArg = context.get("plugin");

        String pluginId = plugin.getPluginManager().getPluginId(pluginArg);
        PluginWatchResults watchResults = plugin.getWatchManager().unwatchPluginsAssociatedWith(pluginId);
        watchResults.sendTo(sender);
        debugCommand("handleUnwatchPlugin completed for plugin '" + pluginId + "'.");
    }

    private void handlePluginInfo(CommandContext<C> context) {
        debugCommand("handlePluginInfo started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        P pluginArg = context.get("plugin");

        createInfo(sender, "plugininfo", pluginArg, this::createPluginInfo);
        debugCommand("handlePluginInfo completed.");
    }

    protected abstract KeyValueComponentBuilder createPluginInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            P pluginArg
    );

    private void handleCommandInfo(CommandContext<C> context) {
        debugCommand("handleCommandInfo started. raw='" + context.getRawInputJoined() + "'.");
        C sender = context.getSender();
        String commandName = context.get("command");

        if (!plugin.getPluginManager().getCommands().contains(commandName)) {
            debugCommand("handleCommandInfo stopped: command not found -> '" + commandName + "'.");
            plugin.getMessagesResource().get(MessageKey.COMMANDINFO_NOT_EXISTS).sendTo(sender);
            return;
        }

        createInfo(sender, "commandinfo", commandName, this::createCommandInfo);
        debugCommand("handleCommandInfo completed for command '" + commandName + "'.");
    }

    protected abstract KeyValueComponentBuilder createCommandInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            String commandName
    );

    private <T> void createInfo(C sender, String command, T item, InfoCreator<T> creator) {
        MessagesResource messages = plugin.getMessagesResource();

        MessagesResource.Message formatMessage = messages.get(command + ".format");
        MessagesResource.Message listFormatMessage = messages.get(command + ".list-format");
        Component separator = messages.get(command + ".list-separator").toComponent();
        Component lastSeparator = messages.get(command + ".list-last-separator").toComponent();

        sender.sendMessage(messages.get(command + ".header").toComponent());
        creator.createInfo(
                KeyValueComponentBuilder.create(formatMessage, "key", "value"),
                listBuilderConsumer -> {
                    ListComponentBuilder<String> listBuilder = ListComponentBuilder.<String>create()
                            .format(str -> listFormatMessage.toComponent(TagResolver.resolver("value",
                                    Tag.inserting(Component.text(str)))))
                            .separator(separator)
                            .lastSeparator(lastSeparator)
                            .emptyValue(null);
                    listBuilderConsumer.accept(listBuilder);
                    return listBuilder.build();
                },
                item
        ).build().forEach(sender::sendMessage);
        sender.sendMessage(messages.get(command + ".footer").toComponent());
    }

    private interface InfoCreator<T> {

        KeyValueComponentBuilder createInfo(
                KeyValueComponentBuilder builder,
                Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
                T item
        );
    }
}
