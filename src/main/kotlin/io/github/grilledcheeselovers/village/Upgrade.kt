package io.github.grilledcheeselovers.village

sealed class Upgrade<T>(
    val id: String,
    val name: String,
    val type: UpgradeType<T>,
    val costCalculator: (level: Int) -> Double,
    val upgradeCalculator: (level: Int) -> T
)

data object VillageRadiusUpgrade : Upgrade<Int>(
    "village_radius",
    "<aqua>Village Radius",
    UpgradeType.IntUpgrade,
    { level -> level * 5.0 }, // todo
    { level -> level } // todo
)

sealed class UpgradeType<T> {

    data object IntUpgrade : UpgradeType<Int>()

}

