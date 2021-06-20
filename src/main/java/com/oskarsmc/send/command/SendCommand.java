package com.oskarsmc.send.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.velocity.arguments.PlayerArgument;
import cloud.commandframework.velocity.arguments.ServerArgument;
import com.oskarsmc.send.Send;
import com.oskarsmc.send.sendable.Sendable;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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

        plugin.commandManager.command(builder.literal("all", ArgumentDescription.of("Send all the players on a server to a server."))
                .argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players to."))
                .handler(context -> {
                    RegisteredServer registeredServer = context.get("server");

                    Sendable sendable = new Sendable(Sendable.Type.PLAYERS, new ArrayList<Player>(proxyServer.getAllPlayers()));
                    this.sendMessage(sendable, context);
                    this.incrementStats(sendable.type(), sendable.players().size());
                    sendable.send(registeredServer);
                })
        );

        if (proxyServer.getPluginManager().getPlugin("luckperms").isPresent()) {
            CommandArgument.Builder<CommandSource, String> groupBuilder = StringArgument.<CommandSource>newBuilder("group-name")
                    .withSuggestionsProvider((commandContext, source) -> {
                        LuckPerms luckPerms = LuckPermsProvider.get();
                        final ArrayList<String> suggestions = new ArrayList<String>();
                        luckPerms.getGroupManager().getLoadedGroups().forEach(group -> suggestions.add(group.getName()));

                        return suggestions;
                    })
                    .withParser((commandContext, inputQueue) -> {
                        String input = inputQueue.peek();

                        LuckPerms luckPerms = LuckPermsProvider.get();

                        if (input == null) { // I dont think this is necessary but theres probably some edge case.
                            return ArgumentParseResult.failure(new IllegalArgumentException("The group you provided is null."));
                        }

                        Group group = luckPerms.getGroupManager().getGroup(input);

                        if (group != null) {
                            return ArgumentParseResult.success(group.getName());
                        }

                        return ArgumentParseResult.failure(new IllegalArgumentException(input + " is not a loaded group."));
                    });

            plugin.commandManager.command(builder.literal("group", ArgumentDescription.of("Send a group of players to a server."))
                    .argument(groupBuilder.build(), ArgumentDescription.of("The name of the group that contains the players to send."))
                    .argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players in the group to."))
                    .handler(context -> {
                        String group = context.get("group-name");
                        RegisteredServer registeredServer = context.get("server");
                        LuckPerms luckPerms = LuckPermsProvider.get();

                        Sendable sendable;

                        luckPerms.getGroupManager().getGroup(group);

                        ArrayList<Player> players = new ArrayList<Player>();

                        for (Player player : proxyServer.getAllPlayers()) {
                            if (player.hasPermission("group." + group)) {
                                players.add(player);
                            }
                        }

                        sendable = new Sendable(Sendable.Type.PLAYERS, players);

                        this.sendMessage(sendable, context);
                        this.incrementStats(sendable.type(), sendable.players().size());
                        sendable.send(registeredServer);
                    })
            );
        } else {
            plugin.logger.warn("LuckPerms was not found, group feature cannot be used.");
        }

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
