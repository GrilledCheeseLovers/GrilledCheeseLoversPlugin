package io.github.grilledcheeselovers.listener

import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.getSafeMessage
import io.github.grilledcheeselovers.constant.sendCoordinateActionBar
import io.github.grilledcheeselovers.extension.relative
import io.github.grilledcheeselovers.item.MAGIC_FISH_ITEM
import io.github.grilledcheeselovers.trading.getWanderingTraderTrades
import io.github.grilledcheeselovers.user.DeathScoreboard
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.util.getCoordinatesColor
import io.github.grilledcheeselovers.util.getPotionBiome
import io.github.grilledcheeselovers.util.isDeathChest
import io.github.grilledcheeselovers.util.setDeathChest
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataHolder
import kotlin.math.cos
import kotlin.math.sin

private val FACES = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
private val BLOCK_FACE_TO_CHEST_DIRECTION = mapOf(
    BlockFace.NORTH to BlockFace.EAST,
    BlockFace.EAST to BlockFace.SOUTH,
    BlockFace.SOUTH to BlockFace.WEST,
    BlockFace.WEST to BlockFace.NORTH
)
private const val SINGLE_CHEST_SIZE = 27

class PlayerListeners(
    private val userManager: UserManager,
    private val deathScoreboard: DeathScoreboard
) : Listener {

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        getCoordinatesColor(player)?.apply { userManager.createUser(player.uniqueId, this) }
        this.deathScoreboard.sendScoreboard(player)
        if (player.hasPlayedBefore()) return
        giveFish(player)
    }

    @EventHandler
    private fun onPlayerLeave(event: PlayerQuitEvent) {
        this.userManager.removeUser(event.player.uniqueId)
    }

    private fun giveFish(player: Player) {
        player.inventory.addItem(MAGIC_FISH_ITEM)
    }

    @EventHandler
    private fun onBlockExplode(event: BlockExplodeEvent) {
        handleDeathChestDrop(event.blockList())
    }

    @EventHandler
    private fun onEntityExplode(event: EntityExplodeEvent) {
        handleDeathChestDrop(event.blockList())
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val state = block.state as? Chest ?: return
        if (!isDeathChest(state)) {
            return
        }
        for (item in state.blockInventory.contents) {
            block.world.dropItem(block.location, item ?: continue)
        }
        block.type = Material.AIR
    }

    private fun handleDeathChestDrop(blocks: MutableCollection<Block>) {
        blocks.removeIf { block ->
            if (!isDeathChest(block.state as? Chest ?: return@removeIf false)) {
                return@removeIf false
            }
            block.type = Material.AIR
            return@removeIf true
        }
    }

    @EventHandler
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val world = player.world
        var chestLoc = player.location
        var placed = false
        var firstChestLoc: Location? = null
        var secondChestLoc: Location? = null

        var usedDirection: BlockFace? = null

        while (!placed) {
            while (!chestLoc.block.type.isAir) {
                chestLoc = chestLoc.relative(BlockFace.UP)
            }
            for (face in FACES) {
                val relative = chestLoc.relative(face)
                if (relative.block.type.isAir) {
                    chestLoc.block.type = Material.CHEST
                    relative.block.type = Material.CHEST
                    firstChestLoc = chestLoc
                    secondChestLoc = relative
                    placed = true
                    usedDirection = face
                    break
                }
            }
            if (!placed) {
                chestLoc = chestLoc.relative(BlockFace.UP)
            }
            if (chestLoc.y >= world.maxHeight) return
        }

        firstChestLoc?.block?.apply {
            setChestDeathData(
                player,
                this,
                org.bukkit.block.data.type.Chest.Type.RIGHT,
                BLOCK_FACE_TO_CHEST_DIRECTION[usedDirection]!!
            )
            addInventoryItems(player, this, 0, SINGLE_CHEST_SIZE)
        }
        secondChestLoc?.block?.apply {
            setChestDeathData(
                player,
                this,
                org.bukkit.block.data.type.Chest.Type.LEFT,
                BLOCK_FACE_TO_CHEST_DIRECTION[usedDirection]!!
            )
            addInventoryItems(player, this, 27, player.inventory.contents.size)
        }
        event.drops.clear()
    }

    private fun setChestDeathData(
        player: Player,
        chest: Block,
        type: org.bukkit.block.data.type.Chest.Type,
        setDirection: BlockFace
    ) {
        val state = chest.state
        if (state !is PersistentDataHolder) return
        setDeathChest(player, state)
        if (state !is Chest) return
        val data = state.blockData as? org.bukkit.block.data.type.Chest ?: return
        data.type = type
        data.facing = setDirection
        state.customName(MINI_MESSAGE.deserialize("<gradient:#19d9ff:#1994ff>${player.name}'s Death Chest"))
        state.update(true)
        chest.setBlockData(data, true)
    }

    private fun addInventoryItems(
        player: Player,
        chest: Block,
        startIndex: Int,
        endIndex: Int
    ) {
        val state = chest.state
        if (state !is InventoryHolder) return
        val contents = player.inventory.contents
        for (i in startIndex until endIndex) {
            state.inventory.setItem(i, contents[i] ?: continue)
        }
    }

    @EventHandler
    private fun onWanderingTraderSpawn(event: EntitySpawnEvent) {
        val entity = event.entity as? WanderingTrader ?: return
        val recipes = entity.recipes.toMutableList()
        recipes.addAll(getWanderingTraderTrades())
        entity.recipes = recipes
    }

    @EventHandler
    private fun onPotionLand(event: PotionSplashEvent) {
        val potion = event.potion
        val item = potion.item
        val biome = getPotionBiome(item.itemMeta ?: return) ?: return
        val location = event.hitBlock ?: return
        val randomRadius = (3..5).random()
        val usedBlocks: MutableSet<Pair<Int, Int>> = mutableSetOf()
        for (radius in (0..randomRadius)) {
            for (angle in (0 until 360)) {
                val x = (location.x + cos(angle.toDouble()) * radius).toInt()
                val z = (location.z + sin(angle.toDouble()) * radius).toInt()
                if (!usedBlocks.add(Pair(x, z))) continue
                for (y in (-radius)..radius) {
                    val newLoc = Location(location.world, x.toDouble(), location.y + y.toDouble(), z.toDouble())
                    newLoc.block.biome = biome
                }
            }
        }
    }

    @EventHandler
    private fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val from = event.from
        val to = event.to
        if (from.x == to.x && from.y == to.y && from.z == to.z) return
        val user = this.userManager[player.uniqueId] ?: return
        user.lastMoved = System.currentTimeMillis()
        val color = user.coordinatesColor ?: return
        sendCoordinateActionBar(player, color)
    }

    @EventHandler
    private fun onPlayerChat(event: AsyncChatEvent) {
        event.renderer { _, displayName, message, _ ->
            getSafeMessage(displayName.append(Component.text(": ")).append(message)) }
    }

    @EventHandler
    private fun onAnvilRename(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val result = (inventory.result ?: return).clone()
        val meta = result.itemMeta ?: return
        meta.displayName(getSafeMessage(meta.displayName() ?: return))
        result.itemMeta = meta
        inventory.result = result
        event.result = result
    }

}