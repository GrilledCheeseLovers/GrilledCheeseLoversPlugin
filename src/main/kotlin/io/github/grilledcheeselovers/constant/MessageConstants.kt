package io.github.grilledcheeselovers.constant

import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

val MINI_MESSAGE = MiniMessage.builder()
    .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
    .build()

val MAGIC_FISH_NAME = MINI_MESSAGE.deserialize("<rainbow>Magic Fish")
