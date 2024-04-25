package io.github.grilledcheeselovers.listener

import io.github.grilledcheeselovers.MINI_MESSAGE
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

class PlayerListeners : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) return
        giveFish(player)
    }

    private fun giveFish(player: Player) {
        val item = ItemStack(Material.COD)
        val meta = item.itemMeta ?: return
        meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true)
        meta.displayName(MINI_MESSAGE.deserialize("<rainbow>Magic Fish"))
        item.setItemMeta(meta)
        player.inventory.addItem(item)
    }

}