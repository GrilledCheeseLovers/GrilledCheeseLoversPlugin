package io.github.grilledcheeselovers.village

import com.destroystokyo.paper.entity.villager.Reputation
import com.destroystokyo.paper.entity.villager.ReputationType
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.ARGUMENT_IS_NOT_NUMBER
import io.github.grilledcheeselovers.constant.BOOST_ALREADY_ACTIVE
import io.github.grilledcheeselovers.constant.BOOST_DOES_NOT_EXIST
import io.github.grilledcheeselovers.constant.ENTERED_VILLAGE_MESSAGE
import io.github.grilledcheeselovers.constant.INVENTORY_FULL
import io.github.grilledcheeselovers.constant.LEFT_VILLAGE_MESSAGE
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.NOT_ENOUGH_WEALTH
import io.github.grilledcheeselovers.constant.PURCHASED_BOOST
import io.github.grilledcheeselovers.constant.PURCHASED_UPGRADE
import io.github.grilledcheeselovers.constant.UNABLE_TO_CREATE_WEALTH_ITEM
import io.github.grilledcheeselovers.constant.UPGRADE_ALREADY_MAXED
import io.github.grilledcheeselovers.constant.UPGRADE_NOT_FOUND
import io.github.grilledcheeselovers.constant.getDepositMessage
import io.github.grilledcheeselovers.item.getWealthItem
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.user.VillageScoreboard
import io.github.grilledcheeselovers.util.getVillagerVillageId
import io.github.grilledcheeselovers.village.logging.VillageLogger
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.util.Collections
import java.util.EnumMap
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
    private var specializationId: String,
    private var wealth: Double = 0.0
) {

    private val logger = VillageLogger(this.plugin, this)
    private val scoreboard = run {
        val scoreboard = VillageScoreboard(this.config, this)
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
        val currentActiveBoost = this.activeBoosts[boost.id]
        if (currentActiveBoost != null && currentActiveBoost.level <= level) {
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

    fun setSpecialization(id: String) {
        this.specializationId = id
        this.scoreboard.updateSpecialization()
    }

    fun getSpecialization(): String {
        return this.specializationId
    }

    fun attemptPurchaseUpgrade(player: Player, upgradeId: String) {
        val upgrade = this.config.getUpgradeById(upgradeId)
        if (upgrade == null) {
            player.sendMessage(UPGRADE_NOT_FOUND)
            return
        }
        val level = this.upgradeLevels[upgradeId] ?: 0
        val nextCost = upgrade.costCalculator(level)
        if (this.wealth < nextCost) {
            player.sendMessage(NOT_ENOUGH_WEALTH)
            return
        }
        if (level >= upgrade.maxLevel) {
            player.sendMessage(UPGRADE_ALREADY_MAXED)
            return
        }
        this.wealth -= nextCost
        this.upgradeLevels[upgradeId] = level + 1
        player.sendMessage(PURCHASED_UPGRADE)
        this.scoreboard.updateWealth()
        this.logger.logUpgradePurchase(player, upgradeId, level + 1)
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
                    sendBoostTimeRanOut(activeBoost)
                    return@removeIf true
                }
                return@removeIf false
            }
        }, 20, 20)
        this.asyncTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, Runnable {
            this.handleBorderSend(userManager)
        }, 20 * 1, 20 * 1)
    }

    private fun sendBoostTimeRanOut(activeBoost: ActiveBoost<*>) {
        val boost = this.config.getBoostById(activeBoost.boostId) ?: return
        for (member in this.members.mapNotNull { Bukkit.getPlayer(it) }) {
            member.sendMessage(MINI_MESSAGE.deserialize("<red>Your village's ${boost.name} <red>boost ran out"))
        }
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
        val height = 2
        for (user in sendTo) {
            val player = Bukkit.getPlayer(user.uuid) ?: continue
            val y = player.y
            drawBorderLine(particle, dustOptions, player, minX, maxX, minZ, minZ, y.toInt(), height)
            drawBorderLine(particle, dustOptions, player, minX, maxX, maxZ, maxZ, y.toInt(), height)
            drawBorderLine(particle, dustOptions, player, minX, minX, minZ, maxZ, y.toInt(), height)
            drawBorderLine(particle, dustOptions, player, maxX, maxX, minZ, maxZ, y.toInt(), height)
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
        when (boost.type) {
            BoostType.PotionEffect -> {
                deactivatePotionBoost(
                    activeBoost as ActiveBoost<PotionBoostData>,
                    boost as Boost<PotionBoostData>,
                    players
                )
            }
            BoostType.VillagerDiscount -> {
                deactivateVillagerDiscountBoost()
            }
            else -> {}
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
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            if (levelData.global) continue
            activateBoost(player, boost, activeBoost)
        }
        player.sendMessage(ENTERED_VILLAGE_MESSAGE)
    }

    fun leave(player: Player) {
        for (activeBoost in this.activeBoosts.values.filter { boost -> this.config.getBoostById(boost.boostId)?.type == BoostType.PotionEffect }) {
            val boost = this.config.getBoostById(activeBoost.boostId) as? Boost<PotionBoostData> ?: continue
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            if (levelData.global) continue
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

            BoostType.VillagerDiscount -> {
                activateVillagerDiscountBoost(boost as Boost<Int>, activeBoost as ActiveBoost<Int>)
            }

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

    private fun activateVillagerDiscountBoost(
        boost: Boost<Int>,
        activeBoost: ActiveBoost<Int>
    ) {
        val world = this.beaconLocation?.world ?: return
        val reputationAmount = boost.levelValues[activeBoost.level]?.value ?: return

        val villagers = world.entities.mapNotNull { it as? Villager ?: return@mapNotNull null }
            .filter { getVillagerVillageId(it) == this.id }
        for (villager in villagers) {
            val reputations: MutableMap<UUID, Reputation> = villager.reputations.toMutableMap()
            for (member in this.members) {
                val currentReputation =
                    reputations[member]
                        ?: Reputation(EnumMap(com.destroystokyo.paper.entity.villager.ReputationType::class.java))
                currentReputation.setReputation(ReputationType.MINOR_POSITIVE, reputationAmount)
                reputations[member] = currentReputation
            }
            reputations.putAll(reputations)
            villager.reputations = reputations
        }
    }

    private fun deactivateVillagerDiscountBoost() {
        val world = this.beaconLocation?.world ?: return

        val villagers = world.entities.mapNotNull { it as? Villager ?: return@mapNotNull null }
            .filter { getVillagerVillageId(it) == this.id }
        for (villager in villagers) {
            val reputations: MutableMap<UUID, Reputation> = villager.reputations.toMutableMap()
            for (member in this.members) {
                val currentReputation =
                    reputations[member]
                        ?: Reputation(EnumMap(com.destroystokyo.paper.entity.villager.ReputationType::class.java))
                currentReputation.setReputation(ReputationType.MINOR_POSITIVE, 0)
                reputations[member] = currentReputation
            }
            reputations.putAll(reputations)
            villager.reputations = reputations
        }
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
        this.scoreboard.initialize()
        this.scoreboard.sendScoreboard(player)
    }

    fun withdrawWealth(player: Player, amount: Double) {
        if (amount < 1) {
            player.sendMessage(ARGUMENT_IS_NOT_NUMBER)
            return
        }
        if (this.wealth < amount) {
            player.sendMessage(NOT_ENOUGH_WEALTH)
            return
        }
        val item = getWealthItem(this, amount)
        if (item == null) {
            player.sendMessage(UNABLE_TO_CREATE_WEALTH_ITEM)
            return
        }
        if (player.inventory.firstEmpty() == -1) {
            player.sendMessage(INVENTORY_FULL)
            return
        }
        this.wealth -= amount
        player.inventory.addItem(item)
        this.logger.logWealthWithdraw(player, amount)
        this.scoreboard.updateWealth()
    }

    fun handlePlayerDeath(player: Player) {
        this.wealth = max(0.0, this.wealth - this.config.getPlayerDeathWealthLoss())
        this.logger.logPlayerDeath(player, this.config.getPlayerDeathWealthLoss())
        this.scoreboard.updateWealth()
    }

    fun handleVillagerDeath(villager: Villager) {
        this.wealth = max(0.0, this.wealth - this.config.getVillagerDeathWealthLoss())
        this.logger.logVillagerDeath(villager, this.config.getVillagerDeathWealthLoss())
        this.scoreboard.updateWealth()
    }

    fun handleVillagerCure(villager: Villager) {
        this.wealth = max(0.0, this.wealth + this.config.getVillagerCureWealthGain())
        this.logger.logVillagerCure(villager, this.config.getVillagerCureWealthGain())
        this.scoreboard.updateWealth()
    }

}

data class VillageSpecialization(val id: String, val name: String, val multiplier: Double, val boostedMaterials: Set<Material>)
