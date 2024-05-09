package io.github.grilledcheeselovers.listener

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.extension.getVillage
import io.github.grilledcheeselovers.village.VillageManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class VillageListeners(
    private val plugin: GrilledCheeseLoversPlugin,
    private val villageManager: VillageManager
) : Listener {

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

}