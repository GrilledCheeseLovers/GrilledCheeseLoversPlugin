package io.github.grilledcheeselovers.util

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

private val DEATH_CHEST_KEY: NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "death_chest"
    )
}
private val BIOME_POTION_KEY: NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "biome_potion"
    )
}


fun setDeathChest(player: Player, chest: PersistentDataHolder) {
    chest.persistentDataContainer.set(DEATH_CHEST_KEY, PersistentDataType.STRING, player.name)
}

fun isDeathChest(chest: PersistentDataHolder): Boolean {
    return chest.persistentDataContainer.has(DEATH_CHEST_KEY)
}

fun setBiomePotion(itemMeta: ItemMeta, biome: Biome) {
    itemMeta.persistentDataContainer.set(BIOME_POTION_KEY, PersistentDataType.STRING, biome.toString())
}

fun getPotionBiome(itemMeta: ItemMeta): Biome? {
    return itemMeta.persistentDataContainer.get(BIOME_POTION_KEY, PersistentDataType.STRING)?.let {
        return Biome.valueOf(it)
    } ?: return null
}