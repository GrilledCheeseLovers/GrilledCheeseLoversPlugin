package io.github.grilledcheeselovers.util

import java.time.Duration


fun formatDuration(duration: Duration): String {
    val secondsLeft = duration.seconds
    return String.format("%d:%02d:%02d", secondsLeft / 3600, (secondsLeft % 3600) / 60, (secondsLeft % 60));
}