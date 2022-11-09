package com.oskarsmc.send;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.CloudInjectionModule;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.google.common.util.concurrent.ExecutionError;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.oskarsmc.send.command.CloudSuggestionProcessor;
import com.oskarsmc.send.command.LuckPermsSendCommandExtension;
import com.oskarsmc.send.command.SendCommand;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.logic.SendDispatcher;
import com.oskarsmc.send.util.SendInjectionModule;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import org.slf4j.Logger;

import java.util.function.Function;

public final class Send {

    @Inject
    private Injector injector;

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        injector = injector.createChildInjector(
                new CloudInjectionModule<>(
                        CommandSource.class,
                        CommandExecutionCoordinator.simpleCoordinator(),
                        Function.identity(),
                        Function.identity()),
                new SendInjectionModule()
        );

        injector.getInstance(Key.get(new TypeLiteral<VelocityCommandManager<CommandSource>>() {
        })).commandSuggestionProcessor(new CloudSuggestionProcessor());

        if (injector.getInstance(SendSettings.class).enabled()) {
            injector.getInstance(SendDispatcher.class);
            injector.getInstance(SendCommand.class);

            try {
                injector.getInstance(LuckPermsSendCommandExtension.class);
            } catch (ExecutionError | NoClassDefFoundError error) {
                logger.warn("LuckPerms was not found, group feature cannot be used.");
            }
        }
    }
}
