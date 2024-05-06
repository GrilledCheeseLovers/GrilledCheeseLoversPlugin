package io.github.grilledcheeselovers.user

import io.github.grilledcheeselovers.constant.DEATH_SCOREBOARD_TITLE
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

private const val DEATH_KEY = "deaths"

class DeathScoreboard {

    private lateinit var scoreboard: Scoreboard
    private lateinit var deathObjective: Objective
    private lateinit var team: Team

    private fun createDeathCounter() {
        this.deathObjective = this.scoreboard.getObjective(DEATH_KEY) ?: this.scoreboard.registerNewObjective(
            DEATH_KEY,
            Criteria.DEATH_COUNT,
            DEATH_SCOREBOARD_TITLE
        )
        this.deathObjective.displaySlot = DisplaySlot.SIDEBAR
        team = this.scoreboard.getTeam(DEATH_KEY) ?: this.scoreboard.registerNewTeam(DEATH_KEY)
        team.color(NamedTextColor.AQUA)
    }

    fun initialize() {
        if (this::scoreboard.isInitialized) return
        this.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        this.createDeathCounter()
    }

    fun sendScoreboard(player: Player) {
        if (!this::scoreboard.isInitialized) {
            return
        }
        if (!this.team.hasEntity(player)) {
            this.deathObjective.getScore(player)?.score = 0
        }
        team.addPlayer(player)
        player.scoreboard = this.scoreboard
    }

}