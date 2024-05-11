package io.github.grilledcheeselovers.user

import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

private const val VILLAGE_NAME_KEY = "village-name"
private const val VILLAGE_WEALTH_KEY = "village-wealth"
private const val VILLAGE_SPECIALIZATION_KEY = "village-specialization"

class VillageScoreboard(
    private val config: GrilledCheeseConfig,
    private val village: Village
) {

    private lateinit var scoreboard: Scoreboard
    private lateinit var boardObjective: Objective
    private lateinit var wealthTeam: Team
    private lateinit var specializationTeam: Team

    private fun createScoreboard() {
        this.scoreboard = Bukkit.getScoreboardManager().newScoreboard
        this.boardObjective = this.scoreboard.registerNewObjective(VILLAGE_NAME_KEY, Criteria.DUMMY, MINI_MESSAGE.deserialize(village.name))
        this.boardObjective.displaySlot = DisplaySlot.SIDEBAR

        this.wealthTeam = this.scoreboard.registerNewTeam(VILLAGE_WEALTH_KEY)
        this.wealthTeam.addEntry("")
        this.wealthTeam.prefix(MINI_MESSAGE.deserialize("<green>Wealth: "))
        this.boardObjective.getScore("").score = this.village.getWealth().toInt()

        this.specializationTeam = this.scoreboard.registerNewTeam(VILLAGE_SPECIALIZATION_KEY)
        this.specializationTeam.addEntry(ChatColor.RED.toString())
        this.specializationTeam.prefix(MINI_MESSAGE.deserialize("<green>Specialization: ${config.getSpecializationById(village.getSpecialization())?.name ?: ""}"))
        this.boardObjective.getScore(ChatColor.RED.toString()).score = 0
    }

    fun updateWealth() {
        this.boardObjective.getScore("").score = this.village.getWealth().toInt()
    }

    fun updateSpecialization() {
        this.specializationTeam.prefix(MINI_MESSAGE.deserialize("<green>Specialization: ${config.getSpecializationById(village.getSpecialization())?.name ?: ""}"))
    }

    fun initialize() {
        if (this::scoreboard.isInitialized) return
        this.createScoreboard()
    }

    fun sendScoreboard(player: Player) {
        if (!this::scoreboard.isInitialized) {
            return
        }
        player.scoreboard = this.scoreboard
    }

}