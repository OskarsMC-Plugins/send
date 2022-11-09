package com.oskarsmc.send.logic;

import com.google.inject.Inject;
import com.oskarsmc.send.Send;
import com.oskarsmc.send.sendable.Sendable;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class SendDispatcher {
    @Inject
    private Send plugin;

    @Inject
    private Scheduler scheduler;

    public SendDispatcher() {

    }

    public void dispatchSendable(@NotNull Sendable sendable, RegisteredServer registeredServer) {
        List<Player> playerList = sendable.players();
        Duration delay = sendable.timeDelay();

        int index = 0;
        for (Player player : playerList) {
            ConnectionRequestBuilder requestBuilder = player.createConnectionRequest(registeredServer);
            scheduler.buildTask(plugin, requestBuilder::fireAndForget)
                    .delay(delay.multipliedBy(index))
                    .schedule();

            index++;
        }
    }
}
