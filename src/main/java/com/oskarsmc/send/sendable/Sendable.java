package com.oskarsmc.send.sendable;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;

public final class Sendable {
    private List<Player> players;
    private final Type sendableType;

    public Sendable(Type sendableType, List<Player> players) {
        this.players = players;
        this.sendableType = sendableType;
    }

    public void send(RegisteredServer registeredServer) {
        this.players.forEach(player -> {
            player.createConnectionRequest(registeredServer).fireAndForget();
        });
    }

    public Type type() {
        return sendableType;
    }

    public List<Player> players() {
        return players;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public boolean removePlayer(Player player) {
        return this.players.remove(player);
    }

    public enum Type {
        PLAYER,
        PLAYERS,
        SERVER,
        UNKNOWN,
    }
}
