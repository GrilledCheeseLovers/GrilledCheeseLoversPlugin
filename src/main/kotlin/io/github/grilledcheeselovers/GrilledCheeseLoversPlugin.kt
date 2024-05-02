package io.github.grilledcheeselovers

import io.github.grilledcheeselovers.listener.PlayerListeners
import org.bukkit.plugin.java.JavaPlugin

class GrilledCheeseLoversPlugin : JavaPlugin() {

    override fun onEnable() {
        this.server.pluginManager.registerEvents(PlayerListeners(), this)
    }

    override fun onDisable() {
    }

}