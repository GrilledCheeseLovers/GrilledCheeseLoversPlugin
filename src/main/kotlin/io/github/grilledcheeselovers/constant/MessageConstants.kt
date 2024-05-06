package io.github.grilledcheeselovers.constant

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.entity.Player


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

val INVALID_COMMAND_USAGE = MINI_MESSAGE.deserialize("<red>Do better.")

val REMOVED_COORDINATES = MINI_MESSAGE.deserialize("<red>No more coordinates for you.")

val DEATH_SCOREBOARD_TITLE = MINI_MESSAGE.deserialize("<rainbow>Player Deaths")