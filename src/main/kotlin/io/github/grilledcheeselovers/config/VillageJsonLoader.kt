package io.github.grilledcheeselovers.config

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.village.ActiveBoost
import io.github.grilledcheeselovers.village.Village
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MEMBERS_KEY = "members"
private const val UPGRADE_LEVELS_KEY = "upgradeLevels"
private const val ACTIVE_BOOSTS_KEY = "activeBoosts"
private const val BOOST_LEVEL_KEY = "level"
private const val BOOST_TIME_LEFT = "timeLeft"

fun loadVillagesFromJson(
    plugin: GrilledCheeseLoversPlugin,
    config: GrilledCheeseConfig,
    villagesString: String
): Map<String, Village> {
    val json = JsonParser.parseString(villagesString) as? JsonObject ?: return emptyMap()
    val villages: MutableMap<String, Village> = hashMapOf()
    for (id in json.keySet()) {
        val villageObject = json.getAsJsonObject(id)
        val members = villageObject.getAsJsonArray(MEMBERS_KEY).toList().map { UUID.fromString(it.asString) }.toSet()
        val upgradeLevelsObject = villageObject.getAsJsonObject(UPGRADE_LEVELS_KEY)
        val upgradeLevels: MutableMap<String, Int> = hashMapOf()
        for (upgradeId in upgradeLevelsObject.keySet()) {
            val level = upgradeLevelsObject.getAsJsonObject(upgradeId).asInt
            upgradeLevels[upgradeId] = level
        }
        val boosts: MutableMap<String, ActiveBoost<*>> = hashMapOf()
        val boostsObject = villageObject.getAsJsonObject(ACTIVE_BOOSTS_KEY)
        for (boostId in boostsObject.keySet()) {
            val boostObject = boostsObject.getAsJsonObject(boostId)
            val level = boostObject.getAsJsonObject(BOOST_LEVEL_KEY).asInt
            val timeLeft = boostObject.getAsJsonObject(BOOST_TIME_LEFT).asInt
            val endTime = LocalDateTime.now().plusSeconds(timeLeft.toLong())
            val boost = config.getBoostById(boostId) ?: continue
            boosts[boostId] = ActiveBoost(boost, boostId, level, endTime)
        }
        val village = Village(
            id,
            plugin,
            config,
            members,
            upgradeLevels,
            boosts
        )
        villages[id] = village
    }
    return villages
}

fun convertVillagesToJson(
    villages: Map<String, Village>
): JsonObject {
    val villagesObject = JsonObject()
    for (village in villages.values) {
        val villageObject = JsonObject()
        val membersArray = JsonArray()
        for (member in village.members) {
            membersArray.add(member.toString())
        }
        villageObject.add(MEMBERS_KEY, membersArray)
        val upgradeLevelsObject = JsonObject()
        for (entry in village.getUpgradeLevels()) {
            upgradeLevelsObject.addProperty(entry.key, entry.value)
        }
        val boostsObject = JsonObject()
        for (entry in village.getActiveBoosts()) {
            val boost = entry.value
            val secondsLeft = Duration.between(LocalDateTime.now(), boost.endTime).seconds
            if (secondsLeft < 0) continue
            val boostObject = JsonObject()
            boostObject.addProperty(BOOST_LEVEL_KEY, boost.level)
            boostObject.addProperty(BOOST_TIME_LEFT, secondsLeft)
            boostsObject.add(entry.key, boostObject)
        }
        villageObject.add(UPGRADE_LEVELS_KEY, upgradeLevelsObject)
        villageObject.add(ACTIVE_BOOSTS_KEY, boostsObject)
        villagesObject.add(village.id, villageObject)
    }
    return villagesObject
}