package io.github.grilledcheeselovers.command

import io.github.grilledcheeselovers.constant.INVALID_COMMAND_USAGE
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.REMOVED_COORDINATES
import io.github.grilledcheeselovers.user.UserManager
import io.github.grilledcheeselovers.util.removeCoordinatesColor
import io.github.grilledcheeselovers.util.setCoordinatesColor
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

private const val COORDINATES_ARG = "coordinates"
private const val NONE_ARG = "none"

private val COLOR_ARGS = NamedTextColor.NAMES.keys().toMutableList().let {
    it.add(NONE_ARG)
    return@let it
}.sorted()

class GrilledCheeseCommand(private val userManager: UserManager) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(INVALID_COMMAND_USAGE)
            return true
        }
        if (sender !is Player) return true
        when (args[0]) {
            COORDINATES_ARG -> {
                if (args.size < 2) {
                    sender.sendMessage(INVALID_COMMAND_USAGE)
                    return true
                }
                val color = args[1]
                if (color == NONE_ARG) {
                    userManager[sender.uniqueId]?.coordinatesColor = null
                    sender.sendMessage(REMOVED_COORDINATES)
                    removeCoordinatesColor(sender)
                    return true
                }
                setCoordinatesColor(sender, color)
                userManager[sender.uniqueId]?.coordinatesColor = color
                sender.sendMessage(MINI_MESSAGE.deserialize("<$color>This is now your coordinate colors."))
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.isEmpty()) {
            return mutableListOf()
        }
        if (args.size == 1 && COORDINATES_ARG.startsWith(args[0].lowercase())) {
            return mutableListOf(COORDINATES_ARG)
        }
        if (args.size == 2 && COORDINATES_ARG == args[0].lowercase()) {
            return COLOR_ARGS.filter { it.startsWith(args[1].lowercase()) }.toMutableList()
        }
        return mutableListOf()
    }
}