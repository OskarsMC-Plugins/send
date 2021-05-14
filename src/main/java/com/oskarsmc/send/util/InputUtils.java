package com.oskarsmc.send.util;

import com.mojang.brigadier.context.CommandContext;
import com.oskarsmc.send.sendable.Sendable;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InputUtils {
    public static Sendable getPlayersToSend(CommandContext<CommandSource> input, ProxyServer proxyServer) {
        String type = input.getArgument("type", String.class);
        String target = input.getArgument("target", String.class);

        if (type.equals("player")) { // Player Input
            if (target.equals("*")) {
                return new Sendable(Sendable.Type.PLAYERS, new ArrayList<Player>(proxyServer.getAllPlayers()));
            }

            Optional<Player> playerOptional = proxyServer.getPlayer(target);
            if (playerOptional.isPresent()) {
                return new Sendable(Sendable.Type.PLAYER, List.of(playerOptional.get()));
            }
        } else if (type.equals("server")) { // Server Input
            Optional<RegisteredServer> serverOptional = proxyServer.getServer(target);
            if (serverOptional.isPresent()) {
                RegisteredServer registeredServer = serverOptional.get();

                return new Sendable(Sendable.Type.SERVER, new ArrayList<Player>(registeredServer.getPlayersConnected()));
            }
        }
        return new Sendable(Sendable.Type.UNKNOWN, new ArrayList<Player>());
    }
}
