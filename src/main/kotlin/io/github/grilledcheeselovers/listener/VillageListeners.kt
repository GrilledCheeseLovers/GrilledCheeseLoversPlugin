package io.github.grilledcheeselovers.listener

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.CANNOT_PLACE_OTHER_VILLAGE_BEACON
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.VILLAGE_ALREADY_HAS_BEACON
import io.github.grilledcheeselovers.constant.getPlacedBeaconMessage
import io.github.grilledcheeselovers.extension.getVillage
import io.github.grilledcheeselovers.menu.sendMainMenu
import io.github.grilledcheeselovers.util.getBeaconVillage
import io.github.grilledcheeselovers.util.getVillagerLastCareerChange
import io.github.grilledcheeselovers.util.getVillagerVillageId
import io.github.grilledcheeselovers.util.isBeaconBlock
import io.github.grilledcheeselovers.util.isVillageVillager
import io.github.grilledcheeselovers.util.setBeaconBlock
import io.github.grilledcheeselovers.util.setPlayerVillageId
import io.github.grilledcheeselovers.util.setVillagerLastCareerChange
import io.github.grilledcheeselovers.util.setVillagerVillageId
import io.github.grilledcheeselovers.village.BoostType
import io.github.grilledcheeselovers.village.Village
import io.github.grilledcheeselovers.village.VillageManager
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.block.Beacon
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Villager
import org.bukkit.entity.ZombieVillager
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTransformEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.inventory.meta.Damageable
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class VillageListeners(
    private val plugin: GrilledCheeseLoversPlugin,
    private val config: GrilledCheeseConfig = plugin.grilledCheeseConfig,
    private val villageManager: VillageManager = plugin.villageManager
) : Listener {

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerVillage = player.getVillage(this.villageManager)
        if (playerVillage != null) {
            playerVillage.sendScoreboard(player)
            playerVillage.enter(player)
            return
        }
        val uuid = player.uniqueId
        for (village in this.villageManager.getVillages().values) {
            if (!village.members.contains(uuid)) continue
            setPlayerVillageId(player, village.id)
            village.sendScoreboard(player)
            village.enter(player)
            break
        }
    }

    @EventHandler
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val village = player.getVillage(this.villageManager) ?: return
        village.handlePlayerDeath(player)
    }

    @EventHandler
    private fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to

        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val player = event.player
        val village = player.getVillage(this.villageManager) ?: return

        val enteringVillage = village.inRadius(to)
        val leavingVillage = village.inRadius(from)
        if (enteringVillage == leavingVillage) return

        if (enteringVillage) {
            village.enter(player)
            return
        }
        village.leave(player)
    }

    @EventHandler
    private fun onBlockPlace(event: BlockPlaceEvent) {
        handleBeaconPlace(event)
    }

    private fun handleBeaconPlace(event: BlockPlaceEvent) {
        val player = event.player
        val item = event.itemInHand
        val villageId = getBeaconVillage(item) ?: return
        val village = this.villageManager.getVillage(villageId) ?: return
        val playerVillage = player.getVillage(this.villageManager) ?: return
        if (villageId != playerVillage.id) {
            player.sendMessage(CANNOT_PLACE_OTHER_VILLAGE_BEACON)
            event.isCancelled = true
            return
        }
        if (village.hasBeacon()) {
            player.sendMessage(VILLAGE_ALREADY_HAS_BEACON)
            event.isCancelled = true
            return
        }
        val block = event.block
        val blockLocation = block.location

        val spawn = event.block.world.spawnLocation
        val distance = spawn.distance(blockLocation)
        if (distance < this.config.getVillageDistanceRequirement()) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>You are too close to spawn ($distance blocks)"))
            event.isCancelled = true
            return
        }
        for (otherVillage in this.villageManager.getVillages().values) {
            if (otherVillage.id == villageId) continue
            val beaconLoc = otherVillage.getBeaconLocation() ?: continue
            val beaconDist = beaconLoc.distance(blockLocation)
            if (beaconDist > this.config.getVillageDistanceRequirement()) continue
            player.sendMessage(MINI_MESSAGE.deserialize("<red>You are too close to another village ($distance blocks)"))
            event.isCancelled = true
            return
        }

        for (member in village.getOnlinePlayers()) {
            member.sendMessage(getPlacedBeaconMessage(block))
        }
        village.setBeacon(block.location)
        val beacon = block.state as Beacon
        setBeaconBlock(beacon)
        spawnVillager(village)
        spawnVillager(village)
    }

    private fun spawnVillager(village: Village) {
        val location = village.getBeaconLocation()?.clone()?.add(0.0, 1.0, 0.0) ?: return
        val world = location.world
        world.spawn(location, Villager::class.java) { villager ->
            setVillagerVillageId(villager, village.id)
            villager.isInvulnerable = true
        }
    }

    @EventHandler
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player
        val village = player.getVillage(this.villageManager) ?: return
        if (!village.isBeacon(block.location)) {
            if (isBeaconBlock(block)) {
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
            }
            return
        }
        if (player.isSneaking && event.item?.type?.isBlock == true) {
            return
        }
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        sendMainMenu(player, this.plugin)
    }

    @EventHandler
    private fun onChunkLoad(event: ChunkLoadEvent) {
        removeExtraVillagers(event.chunk.entities.toList())
    }

    @EventHandler
    private fun onWorldLoad(event: WorldLoadEvent) {
        removeExtraVillagers(event.world.entities)
    }

    private fun removeExtraVillagers(allEntities: Collection<Entity>) {
        val entities = allEntities.filter { entity ->
            if (entity !is Villager) return@filter false
            return@filter !isVillageVillager(entity)
        }
        for (villager in entities) {
            villager.remove()
        }
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (isBeaconBlock(event.block)) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    private fun onItemDamage(event: PlayerItemDamageEvent) {
        val player = event.player
        val item = event.item
        if (item.type.isAir) return
        if (item.itemMeta !is Damageable) return
        val village = player.getVillage(this.plugin.villageManager) ?: return
        var noDamageChance = 0.0
        val inVillage = village.inRadius(player.location)
        for (activeBoost in village.getActiveBoosts().values) {
            val boost = this.config.getBoostById(activeBoost.boostId) ?: continue
            if (boost.type != BoostType.IncreasedDurability) continue
            val level = boost.levelValues[activeBoost.level] ?: continue
            if (!inVillage && !level.global) continue
            val value = level.value ?: continue
            noDamageChance += value as Double
        }
        if (noDamageChance == 0.0) return
        val random = Random.Default.nextDouble()
        if (random < noDamageChance) {
            event.damage = 0
        }
    }

    @EventHandler
    private fun onBlockExplode(event: BlockExplodeEvent) {
        this.handleExplosion(event.blockList())
    }

    @EventHandler
    private fun onEntityExplode(event: EntityExplodeEvent) {
        this.handleExplosion(event.blockList())
    }

    private fun handleExplosion(blocks: MutableList<Block>) {
        blocks.removeIf { block ->
            isBeaconBlock(block)
        }
    }

    @EventHandler
    private fun onVillagerMove(event: EntityMoveEvent) {
        val entity = event.entity as? Villager ?: return
        val villageId = getVillagerVillageId(entity) ?: return
        val village = this.villageManager.getVillage(villageId) ?: return
        val to = event.to
        if (village.inRadius(to)) return
        val beacon = village.getBeaconLocation() ?: return
        entity.teleport(beacon.clone().add(0.0, 1.0, 0.0))
    }

    @EventHandler
    private fun onVillagerTransform(event: EntityTransformEvent) {
        val reason = event.transformReason
        if (reason == EntityTransformEvent.TransformReason.CURED) {
            this.handleCure(event)
            return
        }
        if (reason == EntityTransformEvent.TransformReason.INFECTION) {
            this.handleInfection(event)
            return
        }
    }

    private fun handleInfection(event: EntityTransformEvent) {
        val original = event.transformedEntity as? Villager ?: return
        val villageId = getVillagerVillageId(original) ?: return
        var villager: Villager? = null
        for (entity in event.transformedEntities) {
            val living = entity as? Villager ?: continue
            setVillagerVillageId(living, villageId)
            villager = living
        }
        if (villager == null) return
        val village = this.villageManager.getVillage(villageId) ?: return
        village.handleVillagerDeath(villager)
    }

    private fun handleCure(event: EntityTransformEvent) {
        val original = event.transformedEntity as? ZombieVillager ?: return
        val villageId = getVillagerVillageId(original) ?: return
        var villager: Villager? = null
        for (entity in event.transformedEntities) {
            val living = entity as? Villager ?: continue
            setVillagerVillageId(living, villageId)
            villager = living
        }
        if (villager == null) return
        val village = this.villageManager.getVillage(villageId) ?: return
        village.handleVillagerCure(villager)
    }

    @EventHandler
    private fun onVillagerDeath(event: EntityDeathEvent) {
        val villager = event.entity as? Villager ?: return
        val villageId = getVillagerVillageId(villager) ?: return
        val village = this.villageManager.getVillage(villageId) ?: return
        village.handleVillagerDeath(villager)
    }

    @EventHandler
    private fun onVillagerBreed(event: EntityBreedEvent) {
        val father = event.father as? Villager ?: return
        val mother = event.mother as? Villager ?: return

        val fatherVillageId = getVillagerVillageId(father) ?: return
        val motherVillageId = getVillagerVillageId(mother) ?: return

        if (fatherVillageId != motherVillageId) {
            event.isCancelled = true
            return
        }
        val child = event.entity as? Villager ?: return
        setVillagerVillageId(child, motherVillageId)
    }

    @EventHandler
    private fun onVillagerChooseProfession(event: VillagerCareerChangeEvent) {
        val villager = event.entity
        if (event.profession == Villager.Profession.NONE) return
        val lastChanged = getVillagerLastCareerChange(villager)
        if (lastChanged == null || Duration.between(LocalDateTime.now(), lastChanged.plusSeconds(this.config.getVillagerCareerCooldown().toLong())).isNegative) {
            setVillagerLastCareerChange(villager, LocalDateTime.now())
            return
        }
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockGrow(event: BlockGrowEvent) {
        val block = event.block
        val location = block.location
        val blockData = event.newState.blockData as? Ageable ?: run {
            return
        }
        var village: Village? = null
        for (searchVillage in this.villageManager.getVillages().values) {
            if (!searchVillage.inRadius(location)) continue
            village = searchVillage
            break
        }
        if (village == null) return
        var addAge = 0
        for (activeBoost in village.getActiveBoosts().values) {
            val boost = this.config.getBoostById(activeBoost.boostId) ?: continue
            if (boost.type != BoostType.FasterCrops) continue
            val levelData = boost.levelValues[activeBoost.level] ?: continue
            addAge = max(addAge, levelData.value as Int)
        }
        blockData.age = min(blockData.maximumAge, blockData.age + addAge)
        event.newState.blockData = blockData
    }

    @EventHandler
    private fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity = event.entity
        if (entity.type != EntityType.PHANTOM) return
        val location = entity.location
        var village: Village? = null
        for (searchVillage in this.villageManager.getVillages().values) {
            if (!searchVillage.inRadius(location)) continue
            village = searchVillage
            break
        }
        if (village == null) return
        for (activeBoost in village.getActiveBoosts().values) {
            val boost = this.config.getBoostById(activeBoost.boostId) ?: continue
            if (boost.type != BoostType.NoPhantoms) continue
            event.isCancelled = true
            return
        }
    }

}