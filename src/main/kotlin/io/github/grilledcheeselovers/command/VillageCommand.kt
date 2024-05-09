package io.github.grilledcheeselovers.command

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.ALREADY_HAVE_BEACON_INVENTORY
import io.github.grilledcheeselovers.constant.GIVEN_BEACON
import io.github.grilledcheeselovers.constant.INVALID_COMMAND_USAGE
import io.github.grilledcheeselovers.constant.NOT_IN_VILLAGE
import io.github.grilledcheeselovers.constant.NOT_VIEWING_VILLAGE_BORDER
import io.github.grilledcheeselovers.constant.UNABLE_TO_CREATE_BEACON
import io.github.grilledcheeselovers.constant.VIEWING_VILLAGE_BORDER
import io.github.grilledcheeselovers.constant.VILLAGE_ALREADY_HAS_BEACON
import io.github.grilledcheeselovers.extension.getVillage
import io.github.grilledcheeselovers.item.getBeaconItem
import io.github.grilledcheeselovers.village.VillageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

private const val GET_BEACON_ARG = "getbeacon"
private const val TOGGLE_BORDER_ARG = "toggleborder"

class VillageCommand(
    private val plugin: GrilledCheeseLoversPlugin,
    private val villageManager: VillageManager = plugin.villageManager
) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.isEmpty()) {
            sender.sendMessage(INVALID_COMMAND_USAGE)
            return true
        }
        if (args.size == 1) {
            when (args[0]) {
                GET_BEACON_ARG -> {
                    this.handleGetBeacon(sender)
                }
                TOGGLE_BORDER_ARG -> {
                    this.handleBorderToggle(sender)
                }
            }
        }

        return true
    }

    private fun handleGetBeacon(player: Player) {
        val village = player.getVillage(this.villageManager)
        if (village == null) {
            player.sendMessage(NOT_IN_VILLAGE)
            return
        }
        if (village.hasBeacon()) {
            player.sendMessage(VILLAGE_ALREADY_HAS_BEACON)
            return
        }
        val beacon = getBeaconItem(village)
        if (beacon == null) {
            player.sendMessage(UNABLE_TO_CREATE_BEACON)
            return
        }
        for (member in village.members.mapNotNull { Bukkit.getPlayer(it) }.filter { it.isOnline }) {
            if (member.inventory.containsAtLeast(beacon, 1)) {
                member.sendMessage(ALREADY_HAVE_BEACON_INVENTORY)
                return
            }
        }
        player.inventory.addItem(beacon)
        player.sendMessage(GIVEN_BEACON)
        return
    }

    private fun handleBorderToggle(player: Player) {
        val village = player.getVillage(this.villageManager)
        if (village == null) {
            player.sendMessage(NOT_IN_VILLAGE)
            return
        }
        val user = this.plugin.userManager[player.uniqueId] ?: return
        user.viewingVillageBorder = !user.viewingVillageBorder
        if (user.viewingVillageBorder) {
            player.sendMessage(VIEWING_VILLAGE_BORDER)
        } else {
            player.sendMessage(NOT_VIEWING_VILLAGE_BORDER)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return arrayListOf(
                GET_BEACON_ARG,
                TOGGLE_BORDER_ARG
            )
        }
        return arrayListOf()
    }
}