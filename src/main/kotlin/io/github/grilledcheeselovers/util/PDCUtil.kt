package io.github.grilledcheeselovers.util

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import org.bukkit.NamespacedKey
import org.bukkit.block.Beacon
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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
private val COORDINATE_COLOR_KEY: NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "coordinates_color"
    )
}

private val PLAYER_VILLAGE_KEY: NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "player_village"
    )
}

private val BEACON_ITEM_VILLAGE_KEY: NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "beacon_village"
    )
}

private val BEACON_BLOCK_KEY : NamespacedKey by lazy {
    NamespacedKey(
        JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java),
        "beacon_block"
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

fun getCoordinatesColor(player: Player): String? {
    return player.persistentDataContainer.get(COORDINATE_COLOR_KEY, PersistentDataType.STRING)
}

fun setCoordinatesColor(player: Player, color: String) {
    player.persistentDataContainer.set(COORDINATE_COLOR_KEY, PersistentDataType.STRING, color)
}

fun removeCoordinatesColor(player: Player) {
    player.persistentDataContainer.remove(COORDINATE_COLOR_KEY)
}

fun getPlayerVillageId(player: Player): String? {
    return player.persistentDataContainer.get(PLAYER_VILLAGE_KEY, PersistentDataType.STRING)
}

fun setPlayerVillageId(player: Player, villageId: String) {
    player.persistentDataContainer.set(PLAYER_VILLAGE_KEY, PersistentDataType.STRING, villageId)
}

fun setBeaconVillage(itemMeta: ItemMeta, villageId: String) {
    itemMeta.persistentDataContainer.set(BEACON_ITEM_VILLAGE_KEY, PersistentDataType.STRING, villageId)
}

fun getBeaconVillage(itemStack: ItemStack): String? {
    val meta = itemStack.itemMeta ?: return null
    return meta.persistentDataContainer.get(BEACON_ITEM_VILLAGE_KEY, PersistentDataType.STRING)
}

fun setBeaconBlock(beacon: Beacon) {
    beacon.persistentDataContainer.set(BEACON_BLOCK_KEY, PersistentDataType.BOOLEAN, true)
    beacon.update()
}

fun isBeaconBlock(block: Block): Boolean {
    val state = block.state as? Beacon ?: return false
    return state.persistentDataContainer.get(BEACON_BLOCK_KEY, PersistentDataType.BOOLEAN) ?: false
}