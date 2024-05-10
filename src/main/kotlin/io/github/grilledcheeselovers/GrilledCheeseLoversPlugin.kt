package io.github.grilledcheeselovers

import io.github.grilledcheeselovers.command.GrilledCheeseCommand
import io.github.grilledcheeselovers.command.VillageCommand
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.listener.PlayerListeners
import io.github.grilledcheeselovers.listener.VillageListeners
import io.github.grilledcheeselovers.listener.WorldListener
import io.github.grilledcheeselovers.user.DeathScoreboard
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.village.VillageManager
import io.github.grilledcheeselovers.village.discord.VillageBot
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class GrilledCheeseLoversPlugin : JavaPlugin() {

    val userManager by lazy { UserManager(this) }
    private val deathScoreboard by lazy { DeathScoreboard() }
    val grilledCheeseConfig by lazy { GrilledCheeseConfig(this) }
    val villageManager by lazy { VillageManager() }

    private lateinit var saveTask: BukkitTask

    override fun onEnable() {
        this.userManager.startActionBarTask()
        this.registerCommands()
        this.grilledCheeseConfig.load()
        this.registerListeners()
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            this.grilledCheeseConfig.saveVillages()
        }, 20 * 60, 20 * 60)
        this.setupDiscordBot()
    }

    private fun setupDiscordBot() {
        val token = this.grilledCheeseConfig.getBotToken()
        if (token.isNotBlank()) {
            this.logger.info("Enabling village bot")
            VillageBot(token, this).initializeCommands()
        } else {
            this.logger.info("Bot token not found")
        }
    }
    private fun registerListeners() {
        this.registerListener(PlayerListeners(this.userManager, this.deathScoreboard))
        this.registerListener(WorldListener(this.deathScoreboard))
        this.registerListener(VillageListeners(this))
    }

    private fun registerListener(listener: Listener) {
        this.server.pluginManager.registerEvents(listener, this)
    }

    private fun registerCommands() {
        this.registerCommand("grilledcheese", GrilledCheeseCommand(this.userManager))
        this.registerCommand("village", VillageCommand(this))
    }

    private fun registerCommand(commandName: String, executor: CommandExecutor) {
        this.getCommand(commandName)?.setExecutor(executor)
    }

    override fun onDisable() {
        for (village in villageManager.getVillages().values) {
            village.stop()
        }
        this.userManager.stopActionBarTask()
        this.grilledCheeseConfig.saveVillages()
    }


}