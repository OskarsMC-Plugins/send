package com.oskarsmc.send.sendable;

import com.velocitypowered.api.proxy.Player;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("unused")
public final class Sendable {
    private final List<Player> players;
    private final Type sendableType;
    private Duration timeDelay;

    public Sendable(Type sendableType, List<Player> players, Duration timeDelay) {
        this.players = players;
        this.sendableType = sendableType;
        this.timeDelay = timeDelay;
    }

    public Type type() {
        return sendableType;
    }

    public List<Player> players() {
        return players;
    }

    public Duration timeDelay() {
        return timeDelay;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public boolean removePlayer(Player player) {
        return this.players.remove(player);
    }

    public void timeDelay(Duration timeDelay) {
        this.timeDelay = timeDelay;
    }

    public enum Type {
        PLAYER,
        PLAYERS,
        SERVER,
        UNKNOWN,
    }
}
