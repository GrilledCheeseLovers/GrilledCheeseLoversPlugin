package io.github.grilledcheeselovers

import io.github.grilledcheeselovers.command.GrilledCheeseCommand
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.listener.PlayerListeners
import io.github.grilledcheeselovers.listener.WorldListener
import io.github.grilledcheeselovers.user.DeathScoreboard
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.village.VillageManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class GrilledCheeseLoversPlugin : JavaPlugin() {

    private val userManager by lazy { UserManager(this) }
    private val deathScoreboard by lazy { DeathScoreboard() }
    val grilledCheeseConfig by lazy { GrilledCheeseConfig(this) }
    val villageManger by lazy { VillageManager() }

    private lateinit var saveTask: BukkitTask

    override fun onEnable() {
        this.server.pluginManager.registerEvents(PlayerListeners(this.userManager, this.deathScoreboard), this)
        this.server.pluginManager.registerEvents(WorldListener(this.deathScoreboard), this)
        this.userManager.startActionBarTask()
        this.registerCommands()
        this.grilledCheeseConfig.load()
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            this.grilledCheeseConfig.saveVillages()
        }, 20 * 60, 20 * 60)
    }

    override fun onDisable() {
        this.userManager.stopActionBarTask()
        this.grilledCheeseConfig.saveVillages()
    }

    private fun registerCommands() {
        this.registerCommand("grilledcheese", GrilledCheeseCommand(this.userManager))
    }

    private fun registerCommand(commandName: String, executor: CommandExecutor) {
        this.getCommand(commandName)?.setExecutor(executor)
    }

}