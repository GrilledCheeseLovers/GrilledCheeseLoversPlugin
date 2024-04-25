package io.github.grilledcheeselovers

import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

val MINI_MESSAGE = MiniMessage.builder()
    .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
    .build()
