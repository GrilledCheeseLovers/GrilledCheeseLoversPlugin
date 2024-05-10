package io.github.grilledcheeselovers.village

sealed class Upgrade<T>(
    val id: String,
    val name: String,
    val type: UpgradeType<T>,
    val maxLevel: Int,
    val costCalculator: (level: Int) -> Double,
    val upgradeCalculator: (level: Int) -> T
)

data object VillageRadiusUpgrade : Upgrade<Int>(
    "village_radius",
    "<aqua>Village Radius",
    UpgradeType.IntUpgrade,
    20,
    { level -> 100.0 * level },
    { level -> level * 5 }
)

sealed class UpgradeType<T> {

    data object IntUpgrade : UpgradeType<Int>()

}

