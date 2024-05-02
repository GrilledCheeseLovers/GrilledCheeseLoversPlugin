package io.github.grilledcheeselovers.trading

import io.github.grilledcheeselovers.item.getBiomePotion
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe

private val TRADEABLE_BIOMES = listOf(
    Biome.PLAINS,
    Biome.FOREST,
    Biome.BEACH,
    Biome.TAIGA,
    Biome.DESERT,
    Biome.SNOWY_PLAINS,
    Biome.JUNGLE,
    Biome.MUSHROOM_FIELDS
)

fun getWanderingTraderTrades(): Collection<MerchantRecipe> {
    return listOfNotNull(getBiomePotion(TRADEABLE_BIOMES.random()).let {
        val recipe = MerchantRecipe(it ?: return@let null, (2..5).random())
        recipe.ingredients = listOf(
            ItemStack(Material.EMERALD, (32..64).random()),
            ItemStack(Material.EMERALD, (1..64).random())
        )
        return@let recipe
    })
}

