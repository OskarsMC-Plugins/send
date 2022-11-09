package com.oskarsmc.send.util;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.oskarsmc.send.Send;
import com.oskarsmc.send.command.SendCommand;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.logic.SendDispatcher;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;

public class SendInjectionModule extends AbstractModule {
    protected void configure() {
        bind(SendSettings.class).in(Scopes.SINGLETON);
        bind(SendDispatcher.class).in(Scopes.SINGLETON);
        bind(SendCommand.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public Metrics provideMetrics(@NotNull Injector injector) {
        return injector.getInstance(Metrics.Factory.class).make(injector.getInstance(Send.class), StatUtils.PLUGIN_ID);
    }

    @Provides
    @Singleton
    public Scheduler provideScheduler(@NotNull Injector injector) {
        return injector.getInstance(ProxyServer.class).getScheduler();
    }
}
