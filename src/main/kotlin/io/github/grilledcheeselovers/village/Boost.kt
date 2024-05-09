package io.github.grilledcheeselovers.village

import org.bukkit.potion.PotionEffectType
import java.time.LocalDateTime
import kotlin.time.Duration

sealed class BoostType<T>(val clazz: Class<T>) {

    data object FasterCrops : BoostType<Double>(Double::class.java)
    data object PotionEffect : BoostType<PotionBoostData>(PotionBoostData::class.java)
    data object VillagerDiscount : BoostType<Double>(Double::class.java)
    data object IncreasedDurability : BoostType<Double>(Double::class.java)
    data object NoPhantoms : BoostType<Boolean>(Boolean::class.java)
    data object VillageRadius : BoostType<Int>(Int::class.java)

    companion object {

        private val BOOST_TYPES: Map<String, BoostType<*>> = mapOf(
            "FASTER_CROPS" to FasterCrops,
            "POTION_EFFECT" to PotionEffect,
            "VILLAGER_DISCOUNT" to VillagerDiscount,
            "INCREASED_DURABILITY" to IncreasedDurability,
            "NO_PHANTOMS" to NoPhantoms
        )

        fun getBoostType(key: String): BoostType<*>? {
            return BOOST_TYPES[key]
        }

    }

}

data class Boost<T>(
    val id: String,
    val name: String,
    val type: BoostType<T>,
    val levelValues: Map<Int, BoostLevelData<T>>
)

data class BoostLevelData<T>(val cost: Double, val value: T, val duration: Duration)

class ActiveBoost<T>(
    boost: Boost<T>,
    val boostId: String = boost.id,
    val level: Int,
    val endTime: LocalDateTime = LocalDateTime.now().plusSeconds(boost.levelValues[level]!!.duration.inWholeSeconds)
)


data class PotionBoostData(val effectType: PotionEffectType, val level: Int)
