package io.github.grilledcheeselovers.village.logging

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.DATE_TIME_FORMATTER
import io.github.grilledcheeselovers.village.ActiveBoost
import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemStack
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

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

    fun logUpgradePurchase(player: Player, upgradeId: String, level: Int) {
        this.logMessage("${getPlayerFormat(player)} upgraded $upgradeId to level $level")
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

    fun logPlayerDeath(player: Player, wealthLoss: Double) {
        this.logMessage("${getPlayerFormat(player)} died and the village lost $wealthLoss wealth")
    }

    fun logVillagerDeath(entity: Villager, wealthLoss: Double) {
        this.logMessage("A villager died and the village lost $wealthLoss wealth")
    }

    fun logVillagerCure(entity: Villager, wealthGain: Double) {
        this.logMessage("A villager was cured and the village gained $wealthGain wealth")
    }

    private fun getPlayerFormat(player: Player): String {
        return "${player.name} (${player.uniqueId})"
    }

    private fun logMessage(message: String) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, Runnable {
            Files.writeString(this.villageLogPath, "${DATE_TIME_FORMATTER.format(LocalDateTime.now())} ${message}\n", StandardOpenOption.APPEND)
        })
    }

}