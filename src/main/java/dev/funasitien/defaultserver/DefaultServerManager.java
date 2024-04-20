package dev.funasitien.defaultserver;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DefaultServerManager {
    private final Path dataDirectory;
    private final JdbcDataSource dataSource;
    private final Logger logger;

    @Inject
    public DefaultServerManager(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.dataSource = new JdbcDataSource();
        this.dataSource.setURL("jdbc:h2:./" + this.dataDirectory.resolve("default_server.h2.db").toString());
        logger.info("dataDirectory directory: {}", this.dataDirectory.toAbsolutePath());
        logger.info("dataSource : {}", this.dataSource);
        this.logger = logger;
        initDatabase();
    }

    private void initDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS default_server (player_uuid VARCHAR(36) PRIMARY KEY, server_name VARCHAR(255))");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }

    public void setDefaultServer(UUID playerUuid, String serverName) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "MERGE INTO default_server (player_uuid, server_name) KEY (player_uuid) VALUES (?, ?)")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, serverName);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error setting default server", e);
        }
    }

    public String getDefaultServer(UUID playerUuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT server_name FROM default_server WHERE player_uuid = ?")) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("server_name");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting default server", e);
        }
        return null;
    }
}