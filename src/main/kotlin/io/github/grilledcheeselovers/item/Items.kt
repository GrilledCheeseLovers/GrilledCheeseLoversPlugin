package io.github.grilledcheeselovers.item

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.MAGIC_FISH_NAME
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.getVillageBeaconName
import io.github.grilledcheeselovers.util.setBeaconVillage
import io.github.grilledcheeselovers.util.setBiomePotion
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.util.Locale

val MAGIC_FISH_ITEM: ItemStack by lazy {
    val item = ItemStack(Material.COD)
    val meta = item.itemMeta ?: return@lazy item
    meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true)
    meta.displayName(MAGIC_FISH_NAME)
    item.setItemMeta(meta)
    return@lazy item
}

fun getBeaconItem(village: Village): ItemStack? {
    val item = ItemStack(Material.BEACON)
    val meta = item.itemMeta ?: return null
    meta.displayName(getVillageBeaconName(village.name))
    setBeaconVillage(meta, village.id)
    item.itemMeta = meta
    return item
}

fun getBiomePotion(biome: Biome): ItemStack? {
    return BIOME_POTIONS[biome]
}

private val BIOME_POTIONS: Map<Biome, ItemStack> by lazy {
    return@lazy Biome.entries.associateWith { createBiomePotion(it) }
}

private fun createBiomePotion(biome: Biome): ItemStack {
    val item = ItemStack(Material.SPLASH_POTION)
    val meta = item.itemMeta ?: return item
    meta.displayName(
        MINI_MESSAGE.deserialize(
            "<aqua>${formatBiomeName(biome)} Biome Potion"
        )
    )
    setBiomePotion(meta, biome)
    item.setItemMeta(meta)
    return item
}

private fun formatBiomeName(biome: Biome): String {
    return biome.name.lowercase().replace("_", " ")
        .split(" ").joinToString(" ") { biomeString ->
            biomeString.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
}

