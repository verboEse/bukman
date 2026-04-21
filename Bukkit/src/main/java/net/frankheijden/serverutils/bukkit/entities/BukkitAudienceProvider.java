package net.frankheijden.serverutils.bukkit.entities;

import net.frankheijden.serverutils.bukkit.ServerUtils;
import net.frankheijden.serverutils.common.providers.ServerUtilsAudienceProvider;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BukkitAudienceProvider implements ServerUtilsAudienceProvider<CommandSender> {

    private final ServerUtils plugin;
    private final BukkitAudience consoleServerAudience;

    /**
     * Constructs a new BukkitAudienceProvider.
     */
    public BukkitAudienceProvider(ServerUtils plugin) {
        this.plugin = plugin;
        CommandSender console = plugin.getServer().getConsoleSender();
        this.consoleServerAudience = new BukkitAudience((Audience) console, console);
    }

    @Override
    public BukkitAudience getConsoleServerAudience() {
        return this.consoleServerAudience;
    }

    @Override
    public BukkitAudience get(CommandSender source) {
        return new BukkitAudience((Audience) source, source);
    }

    @Override
    public void broadcast(Component component, String permission) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
        ((Audience) plugin.getServer().getConsoleSender()).sendMessage(component);
    }
}

