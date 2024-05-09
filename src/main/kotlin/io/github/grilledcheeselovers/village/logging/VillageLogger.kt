package io.github.grilledcheeselovers.village.logging

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.village.ActiveBoost
import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class VillageLogger(
    private val plugin: GrilledCheeseLoversPlugin,
    private val village: Village
) {

    private val villageLogPath = this.plugin.dataFolder.toPath().resolve("logs").resolve("${village.id}.log")

    init {
        val file = this.villageLogPath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun logBoostPurchase(player: Player, boost: Boost<*>, level: Int) {
        this.logMessage("${getPlayerFormat(player)} activated boost ${boost.id} at level $level")
    }

    fun logWealthDeposit(player: Player, wealth: Double, item: ItemStack, amount: Int) {
        this.logMessage("${getPlayerFormat(player)} deposited $amount of $item worth $wealth wealth")
    }

    fun logWealthWithdraw(player: Player, wealth: Double) {
        this.logMessage("${getPlayerFormat(player)} withdrew $wealth wealth")
    }

    fun logMaterialDeposit(player: Player, material: Material, amount: Int, worth: Double) {
        this.logMessage("${getPlayerFormat(player)} deposited $amount of $material worth $worth")
    }

    private fun getPlayerFormat(player: Player): String {
        return "${player.name} (${player.uniqueId})"
    }

    private fun logMessage(message: String) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, Runnable {
            Files.writeString(this.villageLogPath, "${message}\n", StandardOpenOption.APPEND)
        })
    }

}