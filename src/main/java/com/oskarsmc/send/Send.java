package com.oskarsmc.send;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.CloudInjectionModule;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.oskarsmc.send.command.SendCommand;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.util.StatUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.Function;

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

    @Inject
    private Injector injector;

    public VelocityCommandManager<CommandSource> commandManager;
    public SendSettings sendSettings;
    public SendCommand sendCommand;
    public Metrics metrics;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        final Injector childInjector = injector.createChildInjector(
                new CloudInjectionModule<>(
                        CommandSource.class,
                        CommandExecutionCoordinator.simpleCoordinator(),
                        Function.identity(),
                        Function.identity()
                )
        );

        this.commandManager = childInjector.getInstance(
                Key.get(new TypeLiteral<VelocityCommandManager<CommandSource>>() {
                })
        );

        this.metrics = metricsFactory.make(this, StatUtils.PLUGIN_ID);

        this.sendSettings = new SendSettings(this.dataDirectory.toFile(), logger);
        this.sendCommand = new SendCommand(this, this.proxyServer);
    }
}
