package io.github.grilledcheeselovers

import io.github.grilledcheeselovers.command.GrilledCheeseCommand
import io.github.grilledcheeselovers.listener.PlayerListeners
import io.github.grilledcheeselovers.user.UserManager
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin

class GrilledCheeseLoversPlugin : JavaPlugin() {

    private val userManager: UserManager by lazy { UserManager(this) }

    override fun onEnable() {
        this.server.pluginManager.registerEvents(PlayerListeners(this.userManager), this)
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