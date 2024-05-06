package io.github.grilledcheeselovers.user

import java.util.UUID


class User(
    val uuid: UUID,
    var lastMoved: Long,
    var coordinatesColor: String?
) {

}