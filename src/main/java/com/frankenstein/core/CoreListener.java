
package com.frankenstein.core;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJoinEvent;

public class CoreListener implements Listener {

    private final FrankensteinCore plugin;

    public CoreListener(FrankensteinCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if (plugin.isMuted(event.getPlayer().getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                "§cУ тебя мут. Писать в чат нельзя."
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.applySavedSize(event.getPlayer());
    }
}
