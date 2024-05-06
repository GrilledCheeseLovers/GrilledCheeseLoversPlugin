package io.github.grilledcheeselovers.user

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.sendCoordinateActionBar
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class UserManager(
    private val plugin: GrilledCheeseLoversPlugin,
    private val users: MutableMap<UUID, User> = HashMap()
) {

    private lateinit var coordinateActionBarTask: BukkitTask

    operator fun get(player: UUID): User? = this.users[player]

    fun createUser(player: UUID, coordinateColor: String?): User {
        val user = User(player, System.currentTimeMillis(), coordinateColor)
        this.users[player] = user
        return user
    }

    fun removeUser(player: UUID) {
        this.users.remove(player)
    }

    fun startActionBarTask() {
        if (this::coordinateActionBarTask.isInitialized) {
            throw IllegalStateException("Actionbar task already initialized")
        }
        this.coordinateActionBarTask = Bukkit.getScheduler().runTaskTimer(this.plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val user = users[player.uniqueId] ?: continue
                val color = user.coordinatesColor ?: continue
                if (System.currentTimeMillis() - user.lastMoved > 1_000) {
                    sendCoordinateActionBar(player, color)
                }
            }
        }, (2 * 20).toLong(), (2 * 20).toLong())
    }

    fun stopActionBarTask() {
        this.coordinateActionBarTask.cancel()
    }

}