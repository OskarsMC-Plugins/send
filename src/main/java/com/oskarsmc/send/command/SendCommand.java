package com.oskarsmc.send.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.LongArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.velocity.VelocityCommandManager;
import cloud.commandframework.velocity.arguments.PlayerArgument;
import cloud.commandframework.velocity.arguments.ServerArgument;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.logic.SendDispatcher;
import com.oskarsmc.send.sendable.Sendable;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SendCommand {
    public AtomicInteger playersSent = new AtomicInteger(0);

    @Inject
    private SendSettings sendSettings;

    public final Command.Builder<CommandSource> commandBuilder;

    public final CommandFlag<Long> durationCommandFlag = CommandFlag
            .newBuilder("delay")
            .withArgument(LongArgument.newBuilder("delay").asOptionalWithDefault(500)) // allow 500ms of delay (2 players per second)
            .build();

    @Inject
    public SendCommand(@NotNull SendSettings sendSettings, @NotNull ProxyServer proxyServer, @NotNull VelocityCommandManager<CommandSource> commandManager, Metrics metrics, SendDispatcher sendDispatcher) {
        commandBuilder = commandManager
                .commandBuilder("send")
                .permission(new SendPermission(sendSettings))
                .flag(durationCommandFlag);

        commandManager.command(commandBuilder.literal("player", ArgumentDescription.of("Send a player to a server.")).argument(PlayerArgument.of("player-name"), ArgumentDescription.of("The name of the player to send.")).argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the player to.")).handler(context -> {
            Player player = context.get("player-name");
            RegisteredServer registeredServer = context.get("server");

            @SuppressWarnings("ConstantConditions") Sendable sendable = new Sendable(Sendable.Type.PLAYER, new ArrayList<>(List.of(player)), Duration.ofSeconds(context.flags().getValue(durationCommandFlag, 0L)));
            this.sendMessage(sendable, context);
            this.incrementStats(sendable.players().size());
            sendDispatcher.dispatchSendable(sendable, registeredServer);
        }));

        commandManager.command(commandBuilder.literal("server", ArgumentDescription.of("Send all the players on a server to a server.")).argument(ServerArgument.of("server-name"), ArgumentDescription.of("The name of a server that contains the players to send.")).argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players to.")).handler(context -> {
            RegisteredServer registeredServerWithPlayers = context.get("server-name");
            RegisteredServer registeredServer = context.get("server");

            @SuppressWarnings("ConstantConditions") Sendable sendable = new Sendable(Sendable.Type.SERVER, new ArrayList<>(registeredServerWithPlayers.getPlayersConnected()), Duration.ofMillis(context.flags().getValue(durationCommandFlag, 0L)));
            this.sendMessage(sendable, context);
            this.incrementStats(sendable.players().size());

            sendDispatcher.dispatchSendable(sendable, registeredServer);
        }));

        commandManager.command(commandBuilder.literal("all", ArgumentDescription.of("Send all the players on a server to a server.")).argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players to.")).handler(context -> {
            RegisteredServer registeredServer = context.get("server");

            @SuppressWarnings("ConstantConditions") Sendable sendable = new Sendable(Sendable.Type.PLAYERS, new ArrayList<>(proxyServer.getAllPlayers()), Duration.ofMillis(context.flags().getValue(durationCommandFlag, 0L)));
            this.sendMessage(sendable, context);
            this.incrementStats(sendable.players().size());
            sendDispatcher.dispatchSendable(sendable, registeredServer);
        }));

        metrics(metrics);
    }

    public void sendMessage(@NotNull Sendable sendable, CommandContext<CommandSource> context) {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        if (sendable.type() == Sendable.Type.UNKNOWN) {
            context.getSender().sendMessage(miniMessage.deserialize(sendSettings.messageRaw("send-no-player")));
        } else if (sendable.type() == Sendable.Type.PLAYER) {
            context.getSender().sendMessage(miniMessage.deserialize(sendSettings.messageRaw("send-success-singular"),
                    Placeholder.parsed("player", sendable.players().get(0).getUsername()),
                    Placeholder.parsed("server", ((RegisteredServer) context.get("server")).getServerInfo().getName())));
        } else if (sendable.type() == Sendable.Type.PLAYERS || sendable.type() == Sendable.Type.SERVER) {
            context.getSender().sendMessage(miniMessage.deserialize(sendSettings.messageRaw("send-success-plural"),
                    Placeholder.parsed("players", "" + sendable.players().size()),
                    Placeholder.parsed("server", ((RegisteredServer) context.get("server")).getServerInfo().getName())));
        }
    }

    public void metrics(@NotNull Metrics metrics) {
        metrics.addCustomChart(new SingleLineChart("players_sent", () -> {
            int ret = playersSent.get();
            playersSent.set(0);
            return ret;
        }));
    }

    public void incrementStats(int players) {
        this.playersSent.addAndGet(players);
    }
}
