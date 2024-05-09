package io.github.grilledcheeselovers.listener

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.CANNOT_PLACE_OTHER_VILLAGE_BEACON
import io.github.grilledcheeselovers.constant.VILLAGE_ALREADY_HAS_BEACON
import io.github.grilledcheeselovers.constant.getPlacedBeaconMessage
import io.github.grilledcheeselovers.extension.getVillage
import io.github.grilledcheeselovers.menu.sendMainMenu
import io.github.grilledcheeselovers.util.getBeaconVillage
import io.github.grilledcheeselovers.util.isBeaconBlock
import io.github.grilledcheeselovers.util.setBeaconBlock
import io.github.grilledcheeselovers.util.setBeaconVillage
import io.github.grilledcheeselovers.util.setPlayerVillageId
import io.github.grilledcheeselovers.village.VillageManager
import org.bukkit.block.Beacon
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent

class VillageListeners(
    private val plugin: GrilledCheeseLoversPlugin,
    private val villageManager: VillageManager = plugin.villageManager
) : Listener {

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerVillage = player.getVillage(this.villageManager)
        if (playerVillage != null) {
            playerVillage.sendScoreboard(player)
            return
        }
        val uuid = player.uniqueId
        for (village in this.villageManager.getVillages().values) {
            if (!village.members.contains(uuid)) continue
            setPlayerVillageId(player, village.id)
            village.sendScoreboard(player)
            break
        }
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
        for (member in village.getOnlinePlayers()) {
            player.sendMessage(getPlacedBeaconMessage(block))
        }
        village.setBeacon(block.location)
        val beacon = block.state as Beacon
        setBeaconBlock(beacon)
    }

    @EventHandler
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player
        if (player.isSneaking) return
        val village = player.getVillage(this.villageManager) ?: return
        if (!village.isBeacon(block.location)) return
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
        sendMainMenu(player, this.plugin)
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (isBeaconBlock(event.block)) {
            event.isCancelled = true
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
}