package io.github.grilledcheeselovers

import io.github.grilledcheeselovers.command.GrilledCheeseCommand
import io.github.grilledcheeselovers.listener.PlayerListeners
import io.github.grilledcheeselovers.listener.WorldListener
import io.github.grilledcheeselovers.user.DeathScoreboard
import io.github.grilledcheeselovers.user.UserManager
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin

class GrilledCheeseLoversPlugin : JavaPlugin() {

    private val userManager by lazy { UserManager(this) }
    private val deathScoreboard by lazy { DeathScoreboard() }

    override fun onEnable() {
        this.server.pluginManager.registerEvents(PlayerListeners(this.userManager, this.deathScoreboard), this)
        this.server.pluginManager.registerEvents(WorldListener(this.deathScoreboard), this)
        this.userManager.startActionBarTask()
        this.registerCommands()
    }

    override fun onDisable() {
        this.userManager.stopActionBarTask()
    }

    private fun registerCommands() {
        this.registerCommand("grilledcheese", GrilledCheeseCommand(this.userManager))
    }

    private fun registerCommand(commandName: String, executor: CommandExecutor) {
        this.getCommand(commandName)?.setExecutor(executor)
    }

}