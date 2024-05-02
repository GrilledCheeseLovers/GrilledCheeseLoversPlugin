package io.github.grilledcheeselovers.extension

import org.bukkit.Location
import org.bukkit.block.BlockFace

fun Location.relative(face:  BlockFace): Location {
    return this.clone().add(face.modX.toDouble(), face.modY.toDouble(), face.modZ.toDouble())
}