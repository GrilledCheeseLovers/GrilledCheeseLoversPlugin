package io.github.grilledcheeselovers.constant

import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.BoostType
import io.github.grilledcheeselovers.village.Village
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


val MINI_MESSAGE = MiniMessage.builder()
    .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
    .build()

private val SAFE_MINI_MESSAGE = MiniMessage.builder()
    .tags(
        TagResolver.builder()
            .resolver(StandardTags.color())
            .resolver(StandardTags.decorations())
            .resolver(StandardTags.gradient())
            .resolver(StandardTags.rainbow())
            .build()
    )
    .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
    .build()

fun getSafeMessage(message: Component): Component {
    return SAFE_MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(message).replace("\\<", "<"))
}

fun sendCoordinateActionBar(player: Player, color: String) {
    val component =
        MINI_MESSAGE.deserialize("<$color>x: ${player.x.toInt()} y: ${player.y.toInt()} z: ${player.z.toInt()}")
    player.sendActionBar(component)
}

val MAGIC_FISH_NAME = MINI_MESSAGE.deserialize("<rainbow>Magic Fish")

fun getVillageBeaconName(villageName: String): Component {
    return MINI_MESSAGE.deserialize("<aqua>Village Beacon for <name>", Placeholder.parsed("name", villageName))
}

fun getPlacedBeaconMessage(block: Block): Component {
    return MINI_MESSAGE.deserialize("<aqua>Your beacon has been placed at (<gold>${block.x}, ${block.y}, ${block.z})")
}

fun getMainMenuName(village: Village): Component {
    return MINI_MESSAGE.deserialize("${village.name}'s Village Menu")
}

val BOOST_TYPES_MENU_NAME = MINI_MESSAGE.deserialize("Boost types")

fun getBoostTypeMenuName(boostType: BoostType<*>): Component {
    return MINI_MESSAGE.deserialize("${boostType.displayName} Boost Type")
}

fun getBoostLevelsMenuName(boost: Boost<*>): Component {
    return MINI_MESSAGE.deserialize("${boost.name} Levels")
}

val ENTERED_VILLAGE_MESSAGE = MINI_MESSAGE.deserialize("<aqua>You entered your village")
val LEFT_VILLAGE_MESSAGE = MINI_MESSAGE.deserialize("<aqua>You left your village")

val INVALID_COMMAND_USAGE = MINI_MESSAGE.deserialize("<red>Do better.")
val NOT_IN_VILLAGE = MINI_MESSAGE.deserialize("<red>You are not in a village")
val UNABLE_TO_CREATE_BEACON = MINI_MESSAGE.deserialize("<red>Could not create your village beacon")
val ALREADY_HAVE_BEACON_INVENTORY = MINI_MESSAGE.deserialize("<red>Someone in your village already has a beacon in their inventory")
val VILLAGE_ALREADY_HAS_BEACON = MINI_MESSAGE.deserialize("<red>Your village already has a beacon")
val CANNOT_PLACE_OTHER_VILLAGE_BEACON = MINI_MESSAGE.deserialize("<red>You cannot place another village's beacon")
val GIVEN_BEACON = MINI_MESSAGE.deserialize("<green>You now have a beacon for your village")

val UPGRADE_NOT_FOUND = MINI_MESSAGE.deserialize("<red>That upgrade was not found")
val UPGRADE_ALREADY_MAXED = MINI_MESSAGE.deserialize("<red>That upgrade already at it's highest level")
val PURCHASED_UPGRADE = MINI_MESSAGE.deserialize("<green>Successfully purchased upgrade")

val REMOVED_COORDINATES = MINI_MESSAGE.deserialize("<red>No more coordinates for you.")

val DEATH_SCOREBOARD_TITLE = MINI_MESSAGE.deserialize("<rainbow>Player Deaths")

val BOOST_DOES_NOT_EXIST = MINI_MESSAGE.deserialize("<red>That boost does not exist")
val NOT_ENOUGH_WEALTH = MINI_MESSAGE.deserialize("<red>You are too poor")
val PURCHASED_BOOST = MINI_MESSAGE.deserialize("<green>You purchased a boost")
val BOOST_ALREADY_ACTIVE = MINI_MESSAGE.deserialize("<red>You already have a boost of that type active")
val ARGUMENT_IS_NOT_NUMBER = MINI_MESSAGE.deserialize("<red>That is not a valid number")
val UNABLE_TO_CREATE_WEALTH_ITEM = MINI_MESSAGE.deserialize("<red>Could not create wealth item")
val INVENTORY_FULL = MINI_MESSAGE.deserialize("<red>Your inventory is full")

val BOOST_MENU_OPTION_ITEM_NAME = MINI_MESSAGE.deserialize("<aqua>Boost Menu")
val MATERIAL_DEPOSIT_MENU_OPTION_ITEM_NAME = MINI_MESSAGE.deserialize("<aqua>Deposit Menu")
val ACTIVE_BOOSTS_MENU_OPTION_ITEM_NAME = MINI_MESSAGE.deserialize("<aqua>Active Boosts Menu")
val UPGRADES_MENU_OPTION_ITEM_NAME = MINI_MESSAGE.deserialize("<aqua>Upgrades Menu")

const val FASTER_CROPS_DISPLAY_NAME = "<aqua>Faster Crops"
const val POTION_EFFECT_DISPLAY_NAME = "<aqua>Potion Effects"
const val VILLAGER_DISCOUNT_DISPLAY_NAME = "<aqua>Villager Trade Discount"
const val INCREASED_DURABILITY_DISPLAY_NAME = "<aqua>Increased Durability"
const val NO_PHANTOMS_DISPLAY_NAME = "<aqua>No Phantoms"

val VIEWING_VILLAGE_BORDER = MINI_MESSAGE.deserialize("<aqua>Now viewing your village border")
val NOT_VIEWING_VILLAGE_BORDER = MINI_MESSAGE.deserialize("<aqua>No longer viewing your village border")

val MATERIAL_DEPOSIT_MENU_NAME = MINI_MESSAGE.deserialize("Deposit Materials")
val ACTIVE_BOOSTS_MENU_NAME = MINI_MESSAGE.deserialize("Active Boosts")
val UPGRADES_MENU_NAME = MINI_MESSAGE.deserialize("Upgrades")

fun getDepositMessage(player: Player, item: ItemStack, worth: Double): Component {
    return MINI_MESSAGE.deserialize("<green>You deposited ${item.amount} of ${item.type.name.lowercase().replace('_', ' ')} worth $worth")
}