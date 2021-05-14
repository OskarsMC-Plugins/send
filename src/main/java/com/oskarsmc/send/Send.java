package com.oskarsmc.send;

import com.google.inject.Inject;
import com.oskarsmc.send.command.SendCommand;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.util.StatUtils;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

public class Send {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxyServer;

    @Inject
    private @DataDirectory
    Path dataDirectory;

    @Inject
    private Metrics.Factory metricsFactory;

    public SendSettings sendSettings;
    public SendCommand sendCommand;
    public Metrics metrics;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.metrics = metricsFactory.make(this, StatUtils.PLUGIN_ID);

        this.sendSettings = new SendSettings(this.dataDirectory.toFile(), logger);
        this.sendCommand = new SendCommand(proxyServer, this.sendSettings, this.metrics);
    }
}
