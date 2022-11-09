package com.oskarsmc.send.command;

import cloud.commandframework.permission.PredicatePermission;
import com.oskarsmc.send.configuration.SendSettings;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public final class SendPermission implements PredicatePermission<CommandSource> {
    private final SendSettings sendSettings;

    @Inject
    public SendPermission(@NotNull SendSettings sendSettings) {
        this.sendSettings = sendSettings;
    }

    @Override
    public boolean hasPermission(@NotNull CommandSource sender) {
        if (sender.hasPermission("osmc.send.send")) {
            if (sender instanceof Player) {
                if (sendSettings.serverBlackListEnabled() && ((Player) sender).getCurrentServer().isPresent()) {
                    return !sendSettings.serversBlackListed().contains(((Player) sender).getCurrentServer().get().getServerInfo().getName());
                }
            }
            return true;
        }
        return false;
    }
}
