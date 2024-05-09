package io.github.grilledcheeselovers.village

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.BOOST_ALREADY_ACTIVE
import io.github.grilledcheeselovers.constant.BOOST_DOES_NOT_EXIST
import io.github.grilledcheeselovers.constant.NOT_ENOUGH_WEALTH
import io.github.grilledcheeselovers.constant.PURCHASED_BOOST
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID
import kotlin.math.absoluteValue

class Village(
    val id: String,
    private val plugin: GrilledCheeseLoversPlugin,
    private val config: GrilledCheeseConfig = plugin.grilledCheeseConfig,
    val members: Set<UUID>,
    private val upgradeLevels: MutableMap<String, Int>,
    private val activeBoosts: MutableMap<String, ActiveBoost<*>>,
    private var wealth: Double = 0.0
) {
    private var beaconLocation: Location? = null

    private lateinit var timer: BukkitTask

    fun <T> attemptPurchaseBoost(player: Player, boost: Boost<T>, level: Int): Boolean {
        val data = boost.levelValues[level] ?: run {
            player.sendMessage(BOOST_DOES_NOT_EXIST)
            return false
        }
        val cost = data.cost
        if (cost > this.wealth) {
            player.sendMessage(NOT_ENOUGH_WEALTH)
            return false
        }
        if (this.activeBoosts[boost.id] != null) {
            player.sendMessage(BOOST_ALREADY_ACTIVE)
            return false
        }
        this.wealth -= cost
        player.sendMessage(PURCHASED_BOOST)
        addBoost(boost, level)
        return true
    }

    fun getUpgradeLevels() : Map<String, Int> {
        return Collections.unmodifiableMap(this.upgradeLevels)
    }

    fun getActiveBoosts(): Map<String, ActiveBoost<*>> {
        return Collections.unmodifiableMap(this.activeBoosts)
    }

    fun <T> addBoost(boost: Boost<T>, level: Int) {
        this.activeBoosts[boost.id] = ActiveBoost(boost, level)
    }

    fun <T> hasBoost(id: String): Boolean {
        return this.activeBoosts.containsKey(id)
    }

    fun getRadius(): Int {
        val radiusLevel = this.upgradeLevels[VillageRadiusUpgrade.id] ?: return this.config.getVillageRadius()
        return this.config.getVillageRadius() + VillageRadiusUpgrade.upgradeCalculator(radiusLevel)
    }

    fun init() {
        if (this::timer.isInitialized) {
            throw IllegalStateException("Village is already initialized!")
        }
        this.timer = Bukkit.getScheduler().runTaskTimer(this.plugin, Runnable {
            this.activeBoosts.entries.removeIf { entry ->
                val activeBoost = entry.value
                if (LocalDateTime.now().isAfter(activeBoost.endTime)) {
                    this.deactivateBoost(activeBoost)
                    return@removeIf true
                }
                return@removeIf false
            }
        }, 1, 1)
    }

    private fun deactivateBoost(activeBoost: ActiveBoost<*>) {
        val players = this.members.map { Bukkit.getPlayer(it) }
            .filter { it != null && it.isOnline }
            .filterNotNull()
        if (players.isEmpty()) return
        if (activeBoost.boost.type == BoostType.PotionEffect) {
            deactivatePotionBoost(activeBoost as ActiveBoost<PotionBoostData>, players)
        }
    }

    private fun deactivatePotionBoost(activeBoost: ActiveBoost<PotionBoostData>, players: Collection<Player>) {
        val boost = activeBoost.boost
        val data = boost.levelValues[activeBoost.level] ?: return
        val effectType = data.value.effectType
        for (player in players) {
            player.removePotionEffect(effectType)
        }
    }

    fun inRadius(location: Location): Boolean {
        val beaconLoc = this.beaconLocation ?: return false
        val radius = this.getRadius()
        val xDistance = (location.blockX - beaconLoc.blockX).absoluteValue
        val zDistance = (location.blockZ - beaconLoc.blockZ).absoluteValue

        return xDistance <= radius && zDistance <= radius
    }

    fun enter(player: Player) {
        for (activeBoost in this.activeBoosts.values.filter { boost -> boost.boost.type == BoostType.PotionEffect }) {
            val boost = activeBoost.boost as Boost<PotionBoostData>
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            player.addPotionEffect(PotionEffect(levelData.value.effectType, Int.MAX_VALUE, levelData.value.level))
        }
    }

    fun leave(player: Player) {
        for (activeBoost in this.activeBoosts.values.filter { boost -> boost.boost.type == BoostType.PotionEffect }) {
            val boost = activeBoost.boost as Boost<PotionBoostData>
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            player.removePotionEffect(levelData.value.effectType)
        }
    }

}