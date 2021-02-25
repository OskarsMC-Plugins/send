package com.oskarsmc.sendvelocity;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

@Plugin(
        id = "sendvelocity",
        name = "SendVelocity",
        version = "0.1.0",
        description = "Plugin to send players to servers on Velocity! ",
        url = "https://software.oskarsmc.com/",
        authors = {"OskarsMC", "OskarZyg"}
)
public class SendVelocity {

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        LiteralCommandNode<CommandSource> sendCommand = LiteralArgumentBuilder
                .<CommandSource>literal("send")
                .executes(context -> {

                    return 1;
                })
                .build();

        ArgumentCommandNode<CommandSource, String> serverNode = RequiredArgumentBuilder
                .<CommandSource, String>argument("server", StringArgumentType.string())
                .suggests((context, builder) -> {
                    for (RegisteredServer server : server.getAllServers()) {
                        builder.suggest(server.getServerInfo().getName());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    if (context.getSource().hasPermission("sendvelocity.send")) {
                        String player = context.getArgument("players", String.class);
                        String server = context.getArgument("server", String.class);

                        Optional<RegisteredServer> serverOptional = this.server.getServer(server);

                        Optional<Player> playerOptional = this.server.getPlayer(player);

                        if (serverOptional.isPresent()) {
                            RegisteredServer registeredServer = serverOptional.get();

                            if (player.equals("*")) {
                                int players = 0;
                                for (Player playerCurrent : this.server.getAllPlayers()) {
                                    playerCurrent.createConnectionRequest(registeredServer).connect();
                                    players++;
                                }
                                context.getSource().sendMessage(Component.text("Sent " + players + " players to " + server + ".").color(NamedTextColor.YELLOW));
                                return 1;
                            }
                            if (playerOptional.isPresent()) {
                                playerOptional.get().createConnectionRequest(registeredServer).connect();
                                context.getSource().sendMessage(Component.text("Sent " + player + " to " + server + ".").color(NamedTextColor.YELLOW));
                                return 1;
                            }

                            context.getSource().sendMessage(Component.text("Sent that player/target doesnt exist.").color(NamedTextColor.YELLOW));
                            return 0;

                        } else {
                            context.getSource().sendMessage(Component.text("That server doesnt exist.").color(NamedTextColor.YELLOW));
                        }
                    }
                    return 0;
                }).build();

        ArgumentCommandNode<CommandSource, String> playerSelectionNode = RequiredArgumentBuilder
                .<CommandSource, String>argument("players", StringArgumentType.string())
                .suggests((context, builder) -> {
                    for (Player player : server.getAllPlayers()) {
                        builder.suggest(player.getUsername());
                    }
                    builder.suggest("\"*\"");
                    return builder.buildFuture();
                })
                .build();

        playerSelectionNode.addChild(serverNode);
        sendCommand.addChild(playerSelectionNode);

        BrigadierCommand sendBrigadier = new BrigadierCommand(sendCommand);


        CommandMeta meta = server.getCommandManager().metaBuilder(sendBrigadier)
                .build();

        server.getCommandManager().register(meta, sendBrigadier);
    }

    
}
