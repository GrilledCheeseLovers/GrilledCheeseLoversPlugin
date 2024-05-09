package io.github.grilledcheeselovers.village

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.BOOST_ALREADY_ACTIVE
import io.github.grilledcheeselovers.constant.BOOST_DOES_NOT_EXIST
import io.github.grilledcheeselovers.constant.ENTERED_VILLAGE_MESSAGE
import io.github.grilledcheeselovers.constant.LEFT_VILLAGE_MESSAGE
import io.github.grilledcheeselovers.constant.NOT_ENOUGH_WEALTH
import io.github.grilledcheeselovers.constant.PURCHASED_BOOST
import io.github.grilledcheeselovers.constant.getDepositMessage
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.user.VillageScoreboard
import io.github.grilledcheeselovers.village.logging.VillageLogger
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class Village(
    val id: String,
    val name: String,
    private val plugin: GrilledCheeseLoversPlugin,
    private val config: GrilledCheeseConfig = plugin.grilledCheeseConfig,
    val members: Set<UUID>,
    private val upgradeLevels: MutableMap<String, Int>,
    private val activeBoosts: MutableMap<String, ActiveBoost<*>>,
    private var wealth: Double = 0.0
) {

    private val logger = VillageLogger(this.plugin, this)
    private val scoreboard = run {
        val scoreboard = VillageScoreboard(this)
        scoreboard.initialize()
        return@run scoreboard
    }

    private var beaconLocation: Location? = null

    private lateinit var timer: BukkitTask
    private lateinit var asyncTimer: BukkitTask

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
        val activeBoost = addBoost(boost, level)
        activateBoost(player, boost, activeBoost)
        this.logger.logBoostPurchase(player, boost, level)
        this.scoreboard.updateWealth()
        return true
    }

    fun depositWealth(player: Player, wealth: Double, item: ItemStack, amount: Int) {
        this.wealth += wealth
        this.logger.logWealthDeposit(player, wealth, item, amount)
        player.sendMessage(getDepositMessage(player, item, wealth))
        this.scoreboard.updateWealth()
    }

    fun getUpgradeLevels(): Map<String, Int> {
        return Collections.unmodifiableMap(this.upgradeLevels)
    }

    fun getActiveBoosts(): Map<String, ActiveBoost<*>> {
        return Collections.unmodifiableMap(this.activeBoosts)
    }

    fun <T> addBoost(boost: Boost<T>, level: Int): ActiveBoost<T> {
        val activeBoost = ActiveBoost(boost, boost.id, level)
        this.activeBoosts[boost.id] = activeBoost
        return activeBoost
    }

    fun <T> hasBoost(id: String): Boolean {
        return this.activeBoosts.containsKey(id)
    }

    fun getRadius(): Int {
        val radiusLevel = this.upgradeLevels[VillageRadiusUpgrade.id] ?: return this.config.getVillageRadius()
        return this.config.getVillageRadius() + VillageRadiusUpgrade.upgradeCalculator(radiusLevel)
    }

    fun init() {
        if (this::timer.isInitialized || this::asyncTimer.isInitialized) {
            throw IllegalStateException("Village is already initialized!")
        }
        val userManager = this.plugin.userManager
        this.timer = Bukkit.getScheduler().runTaskTimer(this.plugin, Runnable {
            this.activeBoosts.entries.removeIf { entry ->
                val activeBoost = entry.value
                if (LocalDateTime.now().isAfter(activeBoost.endTime)) {
                    this.deactivateBoost(activeBoost)
                    return@removeIf true
                }
                return@removeIf false
            }
        }, 20, 20)
        this.asyncTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, Runnable {
            this.handleBorderSend(userManager)
        }, 20 * 1, 20 * 1)
    }

    fun stop() {
        this.timer.cancel()
        this.asyncTimer.cancel()
    }

    private fun handleBorderSend(userManager: UserManager) {
        val beaconLoc = this.beaconLocation ?: return
        val sendTo = this.members.mapNotNull { userManager[it] }
            .filter { it.viewingVillageBorder }
        if (sendTo.isEmpty()) return
        val radius = this.getRadius()
        val startX = beaconLoc.blockX - radius
        val startZ = beaconLoc.blockZ - radius
        val endX = beaconLoc.blockX + radius
        val endZ = beaconLoc.blockZ + radius
        val particle = Particle.REDSTONE
        val dustOptions = DustOptions(Color.fromRGB(0, 255, 247), 1.0f)
        val minX = min(startX, endX)
        val maxX = max(startX, endX)
        val minZ = min(startZ, endZ)
        val maxZ = max(startZ, endZ)
        for (user in sendTo) {
            val player = Bukkit.getPlayer(user.uuid) ?: continue
            val y = player.y
            drawBorderLine(particle, dustOptions, player, minX, maxX, minZ, minZ, y.toInt(), 10)
            drawBorderLine(particle, dustOptions, player, minX, maxX, maxZ, maxZ, y.toInt(), 10)
            drawBorderLine(particle, dustOptions, player, minX, minX, minZ, maxZ, y.toInt(), 10)
            drawBorderLine(particle, dustOptions, player, maxX, maxX, minZ, maxZ, y.toInt(), 10)
        }
    }

    private fun drawBorderLine(
        particle: Particle,
        dustOptions: DustOptions,
        player: Player,
        startX: Int,
        endX: Int,
        startZ: Int,
        endZ: Int,
        startHeight: Int,
        height: Int
    ) {
        for (x in min(startX, endX)..max(startX, endX)) {
            for (z in min(startZ, endZ)..max(startZ, endZ)) {
                for (y in (startHeight - height)..(startHeight + height)) {
                    player.spawnParticle(particle, x.toDouble(), y.toDouble(), z.toDouble(), 1, dustOptions)
                }
            }
        }
    }

    private fun deactivateBoost(activeBoost: ActiveBoost<*>) {
        val players = this.members.map { Bukkit.getPlayer(it) }
            .filter { it != null && it.isOnline }
            .filterNotNull()
        if (players.isEmpty()) return
        val boost = this.config.getBoostById(activeBoost.boostId) ?: run {
            this.plugin.logger.warning("Cannot remove boost ${activeBoost.boostId} because it was not found")
            return
        }
        if (boost.type == BoostType.PotionEffect) {
            deactivatePotionBoost(
                activeBoost as ActiveBoost<PotionBoostData>,
                boost as Boost<PotionBoostData>,
                players
            )
        }
    }

    private fun deactivatePotionBoost(
        activeBoost: ActiveBoost<PotionBoostData>,
        boost: Boost<PotionBoostData>,
        players: Collection<Player>
    ) {
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
        for (activeBoost in this.activeBoosts.values.filter { boost -> this.config.getBoostById(boost.boostId)?.type == BoostType.PotionEffect }) {
            val boost = this.config.getBoostById(activeBoost.boostId) as? Boost<PotionBoostData> ?: continue
            activateBoost(player, boost, activeBoost)
        }
        player.sendMessage(ENTERED_VILLAGE_MESSAGE)
    }

    fun leave(player: Player) {
        for (activeBoost in this.activeBoosts.values.filter { boost -> this.config.getBoostById(boost.boostId)?.type == BoostType.PotionEffect }) {
            val boost = this.config.getBoostById(activeBoost.boostId) as? Boost<PotionBoostData> ?: continue
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            player.removePotionEffect(levelData.value.effectType)
        }
        player.sendMessage(LEFT_VILLAGE_MESSAGE)
    }

    private fun activateBoost(player: Player, boost: Boost<*>, activeBoost: ActiveBoost<*>) {
        when (boost.type) {
            BoostType.PotionEffect -> activatePotionBoost(
                player,
                boost as Boost<PotionBoostData>,
                activeBoost as ActiveBoost<PotionBoostData>
            )

            else -> {

            }
        }
    }

    private fun activatePotionBoost(
        player: Player,
        boost: Boost<PotionBoostData>,
        activeBoost: ActiveBoost<PotionBoostData>
    ) {
        val levelData = boost.levelValues[activeBoost.level] ?: return
        player.addPotionEffect(
            PotionEffect(
                levelData.value.effectType,
                levelData.duration.inWholeSeconds.toInt() * 20,
                levelData.value.level
            )
        )
    }

    fun getOnlinePlayers(): Collection<Player> {
        return this.members.mapNotNull { Bukkit.getPlayer(it) }
    }

    private fun checkBeaconValid() {
        if (this.beaconLocation?.block?.type != Material.BEACON) {
            this.beaconLocation = null
        }
    }

    fun getBeaconLocation(): Location? {
        this.checkBeaconValid()
        return this.beaconLocation
    }

    fun hasBeacon(): Boolean {
        this.checkBeaconValid()
        return this.beaconLocation != null
    }

    fun setBeacon(location: Location) {
        this.beaconLocation = location
    }

    fun isBeacon(location: Location): Boolean {
        this.checkBeaconValid()
        val beacon = this.beaconLocation ?: return false
        return location.blockX == beacon.blockX && location.blockY == beacon.blockY && location.blockZ == beacon.blockZ
    }

    fun getWealth(): Double = this.wealth

    fun sendScoreboard(player: Player) {
        this.scoreboard.sendScoreboard(player)
        player.sendMessage("Sent scoreboard")
    }
}