package com.frankenstein.core;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;

public class CoreListener implements Listener {

    private final FrankensteinCore plugin;

    public CoreListener(FrankensteinCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        String playerName = event.getPlayer().getName();

        if (plugin.isMuted(playerName)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cУ тебя мут. Писать в чат нельзя.");
        }
    }
}
