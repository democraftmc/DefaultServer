package dev.funasitien.defaultserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Singleton
public class Config {
    private static final String CONFIG_FILE_NAME = "config.yml";

    private final Path dataDirectory;
    private final Logger logger;
    private Map<String, Object> configuration;
    private List<String> allowedServers;

    @Inject
    public Config(@DataDirectory Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.loadConfig();
    }

    private void loadConfig() {
        Path configPath = dataDirectory.resolve(CONFIG_FILE_NAME);

        if (Files.notExists(configPath)) {
            createDefaultConfig(configPath);
        }

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            this.configuration = yaml.load(inputStream);
            this.allowedServers = (List<String>) this.configuration.getOrDefault("allowed-servers", List.of());
        } catch (IOException e) {
            logger.error("Error loading configuration file: {}", configPath, e);
            this.configuration = Map.of();
            this.allowedServers = List.of();
        }
    }

    public void reloadConfig() {
        this.loadConfig();
    }

    private void createDefaultConfig(Path configPath) {
        try (InputStream defaultConfig = getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (defaultConfig == null) {
                logger.error("Default configuration file not found: {}", CONFIG_FILE_NAME);
                return;
            }

            Files.createDirectories(dataDirectory);
            Files.copy(defaultConfig, configPath);
            logger.info("Created default configuration file: {}", configPath);
        } catch (IOException e) {
            logger.error("Error creating default configuration file: {}", configPath, e);
        }
    }

    public Object getProperty(String key) {
        return this.configuration.get(key);
    }

    public Object getProperty(String key, Object defaultValue) {
        return this.configuration.getOrDefault(key, defaultValue);
    }

    public List<String> getAllowedServers() {
        return this.allowedServers;
    }
}