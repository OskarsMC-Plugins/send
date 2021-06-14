package com.oskarsmc.send.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.permission.CommandPermission;
import cloud.commandframework.permission.PredicatePermission;
import cloud.commandframework.velocity.arguments.PlayerArgument;
import cloud.commandframework.velocity.arguments.ServerArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.oskarsmc.send.Send;
import com.oskarsmc.send.configuration.SendSettings;
import com.oskarsmc.send.sendable.Sendable;
import com.oskarsmc.send.util.InputUtils;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SendCommand {
    public AtomicInteger playersSent;
    private Send plugin;

    public SendCommand(Send plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.playersSent = new AtomicInteger(0);

        Command.Builder<CommandSource> builder = plugin.commandManager.commandBuilder("send");

        builder = builder.permission(new SendPermission(plugin.sendSettings));

        plugin.commandManager.command(builder.literal("player", ArgumentDescription.of("Send a player to a server."))
                .argument(PlayerArgument.of("player-name"), ArgumentDescription.of("The name of the player to send."))
                .argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the player to."))
                .handler(context -> {
                    Player player = context.get("player-name");
                    RegisteredServer registeredServer = context.get("server");


                    Sendable sendable = new Sendable(Sendable.Type.PLAYER, new ArrayList<Player>(List.of(player)));
                    this.sendMessage(sendable, context);
                    this.incrementStats(sendable.type(), sendable.players().size());
                    sendable.send(registeredServer);
                })
        );

        plugin.commandManager.command(builder.literal("server", ArgumentDescription.of("Send all the players on a server to a server."))
                .argument(ServerArgument.of("server-name"), ArgumentDescription.of("The name of a server that contains the players to send."))
                .argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players to."))
                .handler(context -> {
                    RegisteredServer registeredServerWithPlayers = context.get("server-name");
                    RegisteredServer registeredServer = context.get("server");

                    Sendable sendable = new Sendable(Sendable.Type.SERVER, new ArrayList<Player>(registeredServerWithPlayers.getPlayersConnected()));
                    this.sendMessage(sendable, context);
                    this.incrementStats(sendable.type(), sendable.players().size());
                    sendable.send(registeredServer);
                })
        );

        /*
        plugin.commandManager.command(builder.literal("group", ArgumentDescription.of("Send a group of players to a server."))
                .argument()
        );
         */

        metrics(plugin.metrics);
    }

    public void sendMessage(Sendable sendable, CommandContext<CommandSource> context) {
        if (sendable.type() == Sendable.Type.UNKNOWN) {
            context.getSender().sendMessage(plugin.sendSettings.getMessageParsed("send-no-player"));
        } else if (sendable.type() == Sendable.Type.PLAYER) {
            context.getSender().sendMessage(MiniMessage.get().parse(plugin.sendSettings.getMessageRaw("send-success-singular"), Map.of("player", sendable.players().get(0).getUsername(), "server", ((RegisteredServer) context.get("server")).getServerInfo().getName())));
        } else if (sendable.type() == Sendable.Type.PLAYERS || sendable.type() == Sendable.Type.SERVER) {
            context.getSender().sendMessage(MiniMessage.get().parse(plugin.sendSettings.getMessageRaw("send-success-plural"), Map.of("players", "" + sendable.players().size(), "server", ((RegisteredServer) context.get("server")).getServerInfo().getName())));
        }
    }

    public void metrics(Metrics metrics) {
        metrics.addCustomChart(new SingleLineChart("players_sent", new Callable<Integer>() {
            @Override
            public Integer call() {
                int ret = playersSent.get();
                playersSent.set(0);
                return ret;
            }
        }));
    }

    public void incrementStats(Sendable.Type type, int players) {
        // Player Chart
        this.playersSent.addAndGet(players);
    }
}
