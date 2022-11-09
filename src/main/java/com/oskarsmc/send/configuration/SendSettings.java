package com.oskarsmc.send.configuration;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.oskarsmc.send.util.VersionUtils;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SendSettings {
    private final Path dataFolder;
    private final Path configPath;

    private Boolean serverBlackListEnabled;
    private List<String> serversBlackListed;

    private Toml messages;

    private final Double configVersion;
    private boolean enabled;

    @Inject
    public SendSettings(@DataDirectory @NotNull Path dataDirectory, Logger logger) {
        this.dataFolder = dataDirectory;
        this.configPath = dataDirectory.resolve("config.toml");

        try {
            saveDefaultConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Toml toml = loadConfig();

        this.enabled = toml.getBoolean("plugin.enabled");

        // Version
        this.configVersion = toml.getDouble("developer-info.config-version");

        if (!VersionUtils.isLatestConfigVersion(this)) {
            logger.warn("Your Config is out of date (Latest: " + VersionUtils.CONFIG_VERSION + ", Config Version: " + this.configVersion() + ")!");
            logger.warn("Please backup your current config.toml, and delete the current one. A new config will then be created on the next proxy launch.");
            logger.warn("The plugin's functionality will not be enabled until the config is updated.");
            this.enabled(false);
            return;
        }

        this.messages = toml.getTable("messages");

        this.serverBlackListEnabled = toml.getBoolean("servers.server-blacklist");
        this.serversBlackListed = toml.getList("servers.servers-blacklisted");
    }

    private void saveDefaultConfig() throws IOException {
        if (Files.notExists(dataFolder)) Files.createDirectory(dataFolder);
        if (Files.exists(configPath)) return;

        try (InputStream in = SendSettings.class.getResourceAsStream("/config.toml")) {
            assert in != null;
            Files.copy(in, configPath);
        }
    }

    @Contract(" -> new")
    private @NotNull File configFile() {
        return configPath.toFile();
    }

    private Toml loadConfig() {
        return new Toml().read(configFile());
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Double configVersion() {
        return configVersion;
    }

    public String messageRaw(String key) {
        return this.messages.getString(key);
    }

    public Boolean serverBlackListEnabled() {
        return serverBlackListEnabled;
    }

    public List<String> serversBlackListed() {
        return serversBlackListed;
    }
}
