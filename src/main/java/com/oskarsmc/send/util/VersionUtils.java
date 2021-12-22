package com.oskarsmc.send.util;

import com.moandjiezana.toml.Toml;
import com.oskarsmc.send.configuration.SendSettings;
import org.jetbrains.annotations.NotNull;

public final class VersionUtils {
    public static final double CONFIG_VERSION = getDefaultConfiguration().getDouble("developer-info.config-version");

    public static boolean isLatestConfigVersion(@NotNull SendSettings sendSettings) {
        if (sendSettings.getConfigVersion() == null) {
            return false;
        }
        return sendSettings.getConfigVersion() == CONFIG_VERSION;
    }

    public static Toml getDefaultConfiguration() {
        return new Toml().read(VersionUtils.class.getResourceAsStream("/config.toml"));
    }
}
