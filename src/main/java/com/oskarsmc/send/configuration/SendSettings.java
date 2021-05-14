package com.oskarsmc.send.configuration;

import com.moandjiezana.toml.Toml;
import com.oskarsmc.send.util.VersionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

public class SendSettings {
    private final File dataFolder;
    private final File file;

    private Boolean serverBlackListEnabled;
    private List<String> serversBlackListed;

    private Toml messages;

    private final Double configVersion;
    private boolean enabled;

    public SendSettings(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.file = new File(this.dataFolder, "config.toml");

        saveDefaultConfig();
        Toml toml = loadConfig();

        this.enabled = toml.getBoolean("plugin.enabled");

        // Version
        this.configVersion = toml.getDouble("developer-info.config-version");

        if (!VersionUtils.isLatestConfigVersion(this)) {
            logger.warn("Your Config is out of date (Latest: " + VersionUtils.CONFIG_VERSION + ", Config Version: " + this.getConfigVersion() + ")!");
            logger.warn("Please backup your current config.toml, and delete the current one. A new config will then be created on the next proxy launch.");
            logger.warn("The plugin's functionality will not be enabled until the config is updated.");
            this.setEnabled(false);
            return;
        }

        this.messages = toml.getTable("messages");

        this.serverBlackListEnabled = toml.getBoolean("servers.server-blacklist");
        this.serversBlackListed = toml.getList("servers.servers-blacklisted");
    }

    private void saveDefaultConfig() {
        if (!dataFolder.exists()) dataFolder.mkdir();
        if (file.exists()) return;

        try (InputStream in = SendSettings.class.getResourceAsStream("/config.toml")) {
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getConfigFile() {
        return new File(dataFolder, "config.toml");
    }

    private Toml loadConfig() {
        return new Toml().read(getConfigFile());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Double getConfigVersion() {
        return configVersion;
    }

    public Component getMessageParsed(String key) {
        return MiniMessage.get().parse(this.messages.getString(key));
    }

    public String getMessageRaw(String key) {
        return this.messages.getString(key);
    }

    public Boolean getServerBlackListEnabled() {
        return serverBlackListEnabled;
    }

    public List<String> getServersBlackListed() {
        return serversBlackListed;
    }
}
