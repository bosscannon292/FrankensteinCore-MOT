package com.frankenstein.core;

import cn.nukkit.plugin.PluginBase;

public class FrankensteinCore extends PluginBase {

    @Override
    public void onEnable() {
        getLogger().info("FrankensteinCore успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FrankensteinCore выключен!");
    }
}
