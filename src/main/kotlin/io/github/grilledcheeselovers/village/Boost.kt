package io.github.grilledcheeselovers.village

import io.github.grilledcheeselovers.constant.FASTER_CROPS_DISPLAY_NAME
import io.github.grilledcheeselovers.constant.INCREASED_DURABILITY_DISPLAY_NAME
import io.github.grilledcheeselovers.constant.NO_PHANTOMS_DISPLAY_NAME
import io.github.grilledcheeselovers.constant.POTION_EFFECT_DISPLAY_NAME
import io.github.grilledcheeselovers.constant.VILLAGER_DISCOUNT_DISPLAY_NAME
import org.bukkit.potion.PotionEffectType
import java.time.LocalDateTime
import kotlin.time.Duration

sealed class BoostType<T>(val clazz: Class<T>, val displayName: String) {

    data object FasterCrops : BoostType<Int>(Int::class.java, FASTER_CROPS_DISPLAY_NAME)
    data object PotionEffect : BoostType<PotionBoostData>(PotionBoostData::class.java, POTION_EFFECT_DISPLAY_NAME)
    data object VillagerDiscount : BoostType<Int>(Int::class.java, VILLAGER_DISCOUNT_DISPLAY_NAME)
    data object IncreasedDurability : BoostType<Double>(Double::class.java, INCREASED_DURABILITY_DISPLAY_NAME)
    data object NoPhantoms : BoostType<Boolean>(Boolean::class.java, NO_PHANTOMS_DISPLAY_NAME)

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

data class BoostLevelData<T>(val cost: Double, val value: T, val global: Boolean, val duration: Duration)

class ActiveBoost<T>(
    boost: Boost<T>,
    val boostId: String = boost.id,
    val level: Int,
    val endTime: LocalDateTime = LocalDateTime.now().plusSeconds(boost.levelValues[level]!!.duration.inWholeSeconds)
)


data class PotionBoostData(val effectType: PotionEffectType, val level: Int)
