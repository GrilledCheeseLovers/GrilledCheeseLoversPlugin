package io.github.grilledcheeselovers.extension

import io.github.grilledcheeselovers.util.getPlayerVillageId
import io.github.grilledcheeselovers.village.Village
import io.github.grilledcheeselovers.village.VillageManager
import org.bukkit.entity.Player

fun Player.getVillage(villageManager: VillageManager): Village? {
    val id = getPlayerVillageId(this) ?: return null
    return villageManager.getVillage(id)
}