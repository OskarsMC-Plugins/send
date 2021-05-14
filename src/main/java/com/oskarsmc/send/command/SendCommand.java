package com.oskarsmc.send.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
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

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SendCommand {
    public ConcurrentMap<String, AtomicInteger> sendType;
    public AtomicInteger playersSent;

    public String sendPerm = "osmc.send.send";

    public SendCommand(ProxyServer proxyServer, SendSettings sendSettings, Metrics metrics) {

        this.sendType = new ConcurrentHashMap<String, AtomicInteger>();
        this.playersSent = new AtomicInteger(0);

        LiteralCommandNode<CommandSource> sendCommand = LiteralArgumentBuilder
                .<CommandSource>literal("send")
                .executes(context -> {
                    if (context.getSource().hasPermission(sendPerm)) {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("send-usage"));
                    } else {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("no-permission"));
                    }
                    return 1;
                })
                .build();

        ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
                .<CommandSource, String>argument("server", StringArgumentType.string())
                .suggests((context, builder) -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        for (RegisteredServer server : proxyServer.getAllServers()) {
                            builder.suggest(server.getServerInfo().getName());
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        String target = context.getArgument("target", String.class);
                        String server = context.getArgument("server", String.class);

                        Optional<RegisteredServer> serverOptional = proxyServer.getServer(server);

                        if (serverOptional.isPresent()) {
                            Sendable sendable = InputUtils.getPlayersToSend(context, proxyServer);

                            if (sendable.type() == Sendable.Type.UNKNOWN) {
                                context.getSource().sendMessage(sendSettings.getMessageParsed("send-no-player"));
                                return 0;
                            } else if (sendable.type() == Sendable.Type.PLAYER) {
                                context.getSource().sendMessage(MiniMessage.get().parse(sendSettings.getMessageRaw("send-success-singular"), Map.of("player", sendable.players().get(0).getUsername(), "server", server)));
                            } else if (sendable.type() == Sendable.Type.PLAYERS || sendable.type() == Sendable.Type.SERVER) {
                                context.getSource().sendMessage(MiniMessage.get().parse(sendSettings.getMessageRaw("send-success-plural"), Map.of("players", "" + sendable.players().size(), "server", server.replace("s:", ""))));
                            }

                            incrementStats(sendable.type(), sendable.players().size());

                            sendable.send(serverOptional.get());
                        } else {
                            context.getSource().sendMessage(sendSettings.getMessageParsed("send-no-server"));
                        }
                    } else {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("no-permission"));
                    }
                    return 0;
                }).build();

        ArgumentCommandNode<CommandSource, String> typeSelectionNode = RequiredArgumentBuilder
                .<CommandSource, String>argument("type", StringArgumentType.word())
                .suggests((context, builder) -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        builder.suggest("server");
                        builder.suggest("player");
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("send-usage"));
                    } else {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("no-permission"));
                    }
                    return 0;
                })
                .build();

        ArgumentCommandNode<CommandSource, String> playerSelectionNode = RequiredArgumentBuilder
                .<CommandSource, String>argument("target", StringArgumentType.string())
                .suggests((context, builder) -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        if (context.getArgument("type", String.class).equals("server")) {
                            for (RegisteredServer server : proxyServer.getAllServers()) {
                                builder.suggest(server.getServerInfo().getName());
                            }
                        } else if (context.getArgument("type", String.class).equals("player")) {
                            for (Player player : proxyServer.getAllPlayers()) {
                                builder.suggest(player.getUsername());
                            }
                            builder.suggest("\"*\"");
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    if (checkPerm(context.getSource(), sendSettings)) {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("send-usage"));
                    } else {
                        context.getSource().sendMessage(sendSettings.getMessageParsed("no-permission"));
                    }
                    return 0;
                })
                .build();

        playerSelectionNode.addChild(serverNode);
        typeSelectionNode.addChild(playerSelectionNode);
        sendCommand.addChild(typeSelectionNode);

        BrigadierCommand sendBrigadier = new BrigadierCommand(sendCommand);


        CommandMeta meta = proxyServer.getCommandManager().metaBuilder(sendBrigadier)
                .build();

        proxyServer.getCommandManager().register(meta, sendBrigadier);

        metrics(metrics);
    }

    public void metrics(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("send_type", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, AtomicInteger> temp = sendType;
                Map<String, Integer> ret = new ConcurrentHashMap<>();
                temp.forEach((key, value) -> {
                    ret.put(key, value.get());
                });
                sendType.clear();
                return ret;
            }
        }));

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
        // Type Pie
        AtomicBoolean exists = new AtomicBoolean(false);
        this.sendType.forEach((key, value) -> {
            if (type.name().toLowerCase(Locale.ROOT) ==  key) {
                value.addAndGet(players);
                exists.set(true);
            }
        });
        if (!exists.get()) {
            this.sendType.put(type.name().toLowerCase(Locale.ROOT), new AtomicInteger(players));
        }
        // Player Chart
        this.playersSent.incrementAndGet();
    }

    public boolean checkPerm(CommandSource source, SendSettings sendSettings) {
        if (source.hasPermission("osmc.send.send")) {
            if (source instanceof Player) {
                if (sendSettings.getServerBlackListEnabled() && ((Player) source).getCurrentServer().isPresent()) {
                    if (sendSettings.getServersBlackListed().contains(((Player) source).getCurrentServer().get().getServerInfo().getName())) {
                        return false;
                    } else {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
