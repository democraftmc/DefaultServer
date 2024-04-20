package dev.funasitien.defaultserver;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(id = "defaultserver", name = "DefaultServer", version = "1.0.0", authors = "Funasitien", url = "https://f.dreamclouds.fr")
public class DefaultServer {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final DefaultServerManager defaultServerManager;
    private final Config config;

    @Inject
    public DefaultServer(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory, Config config) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = config;
        this.defaultServerManager = new DefaultServerManager(proxyServer, logger, dataDirectory);



        logger.info("DefaultServer plugin has been enabled!");
        logger.info("Allowed servers: {}", config.getAllowedServers());

        // Register the /defaultserver command
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register("defaultserver", new DefaultServerCommand(), "defaultserver");
        commandManager.register("reloadserver", new DefaultServerReload(), "reloadserver");

    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        String defaultServer = defaultServerManager.getDefaultServer(player.getUniqueId());
        if (defaultServer != null) {
            event.setInitialServer(proxyServer.getServer(defaultServer).orElse(null));
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String defaultServer = defaultServerManager.getDefaultServer(player.getUniqueId());
        if (defaultServer != null) {
            player.createConnectionRequest(proxyServer.getServer(defaultServer).orElse(null)).fireAndForget();
        }
    }

    private class DefaultServerCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (source instanceof Player) {
                Player player = (Player) source;
                if (invocation.arguments().length == 1) {
                    String newDefaultServer = invocation.arguments()[0];
                    if (config.getAllowedServers().contains(newDefaultServer)) {
                        defaultServerManager.setDefaultServer(player.getUniqueId(), newDefaultServer);
                        player.sendMessage(Component.text("Your default server has been set to " + newDefaultServer));
                    } else {
                        player.sendMessage(Component.text("The server '" + newDefaultServer + "' is not allowed."));
                    }
                } else {
                    player.sendMessage(Component.text("Usage: /defaultserver <server>"));
                }
            } else {
                invocation.source().sendMessage(Component.text("This command can only be used by players."));
            }
        }

        @Override
        public List<String> suggest(final Invocation invocation) {
            return config.getAllowedServers();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            // Implement permission check here
            return true;
        }
    }

    private class DefaultServerReload implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            config.reloadConfig();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            // Implement permission check here
            return true;
        }
    }
}