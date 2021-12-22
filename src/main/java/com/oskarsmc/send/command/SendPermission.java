package com.oskarsmc.send.command;

import cloud.commandframework.permission.PredicatePermission;
import com.oskarsmc.send.configuration.SendSettings;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SendPermission implements PredicatePermission<CommandSource> {
    private final SendSettings sendSettings;

    public SendPermission(SendSettings sendSettings) {
        this.sendSettings = sendSettings;
    }

    @Contract(pure = true)
    @Override
    public @Nullable String toString() {
        return null; //TODO: Make it return something if someone relies on this?
    }

    @Override
    public boolean hasPermission(@NotNull CommandSource sender) {
        if (sender.hasPermission("osmc.send.send")) {
            if (sender instanceof Player) {
                if (sendSettings.getServerBlackListEnabled() && ((Player) sender).getCurrentServer().isPresent()) {
                    return !sendSettings.getServersBlackListed().contains(((Player) sender).getCurrentServer().get().getServerInfo().getName());
                }
            }
            return true;
        }
        return false;
    }
}
