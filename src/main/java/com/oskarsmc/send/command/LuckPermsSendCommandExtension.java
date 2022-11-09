package com.oskarsmc.send.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.velocity.VelocityCommandManager;
import cloud.commandframework.velocity.arguments.ServerArgument;
import com.google.inject.Inject;
import com.oskarsmc.send.logic.SendDispatcher;
import com.oskarsmc.send.sendable.Sendable;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;

public class LuckPermsSendCommandExtension {
    @Inject
    public LuckPermsSendCommandExtension(@NotNull VelocityCommandManager<CommandSource> commandManager, @NotNull SendCommand sendCommand, ProxyServer proxyServer, SendDispatcher sendDispatcher) {
        CommandArgument.Builder<CommandSource, String> groupBuilder = StringArgument
                .<CommandSource>newBuilder("group-name")
                .withSuggestionsProvider((commandContext, source) -> {
                    LuckPerms luckPerms = LuckPermsProvider.get();
                    final ArrayList<String> suggestions = new ArrayList<>();
                    luckPerms.getGroupManager().getLoadedGroups().forEach(group -> suggestions.add(group.getName()));

                    return suggestions;
                })
                .withParser((commandContext, inputQueue) -> {
                    String input = inputQueue.peek();

                    LuckPerms luckPerms = LuckPermsProvider.get();

                    if (input == null) { // I don't think this is necessary but there's probably some edge case.
                        return ArgumentParseResult.failure(new IllegalArgumentException("The group you provided is null."));
                    }

                    Group group = luckPerms.getGroupManager().getGroup(input);

                    if (group != null) {
                        return ArgumentParseResult.success(group.getName());
                    }

                    return ArgumentParseResult.failure(new IllegalArgumentException(input + " is not a loaded group."));
                });

        commandManager.command(sendCommand.commandBuilder.literal("group", ArgumentDescription.of("Send a group of players to a server."))
                .argument(groupBuilder.build(), ArgumentDescription.of("The name of the group that contains the players to send."))
                .argument(ServerArgument.of("server"), ArgumentDescription.of("The server to send the players in the group to."))
                .handler(context -> {
                    String group = context.get("group-name");
                    RegisteredServer registeredServer = context.get("server");
                    LuckPerms luckPerms = LuckPermsProvider.get();

                    luckPerms.getGroupManager().getGroup(group);

                    ArrayList<Player> players = new ArrayList<>();

                    for (Player player : proxyServer.getAllPlayers()) {
                        if (player.hasPermission("group." + group)) {
                            players.add(player);
                        }
                    }

                    @SuppressWarnings("ConstantConditions") Sendable sendable = new Sendable(Sendable.Type.PLAYERS, players, Duration.ofMillis(context.flags().getValue(sendCommand.durationCommandFlag, 0L)));

                    sendCommand.sendMessage(sendable, context);
                    sendCommand.incrementStats(sendable.players().size());
                    sendDispatcher.dispatchSendable(sendable, registeredServer);
                }));
    }
}
