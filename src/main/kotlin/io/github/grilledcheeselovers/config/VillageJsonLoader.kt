package io.github.grilledcheeselovers.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.village.ActiveBoost
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

private const val NAME_KEY = "name"
private const val WEALTH_KEY = "wealth"
private const val BEACON_LOCATION_KEY = "beacon-location"
private const val WORLD_UUID_KEY = "world-uuid"
private const val LOCATION_X_KEY = "x"
private const val LOCATION_Y_KEY = "y"
private const val LOCATION_Z_KEY = "z"
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
        val name = villageObject.get(NAME_KEY).asString
        val wealth = villageObject.get(WEALTH_KEY).asDouble
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
            val level = boostObject.get(BOOST_LEVEL_KEY).asInt
            val timeLeft = boostObject.get(BOOST_TIME_LEFT).asInt
            val endTime = LocalDateTime.now().plusSeconds(timeLeft.toLong())
            val boost = config.getBoostById(boostId) ?: continue
            boosts[boostId] = ActiveBoost(boost, boostId, level, endTime)
        }
        val village = Village(
            id,
            name,
            plugin,
            config,
            members,
            upgradeLevels,
            boosts,
            wealth
        )

        if (villageObject.has(BEACON_LOCATION_KEY)) {
            val beaconLocationObject = villageObject.getAsJsonObject(BEACON_LOCATION_KEY)
            val worldUUID = UUID.fromString(beaconLocationObject.get(WORLD_UUID_KEY).asString)
            val locationX = beaconLocationObject.get(LOCATION_X_KEY).asInt
            val locationY = beaconLocationObject.get(LOCATION_Y_KEY).asInt
            val locationZ = beaconLocationObject.get(LOCATION_Z_KEY).asInt
            val beaconLocation =
                Location(Bukkit.getWorld(worldUUID), locationX.toDouble(), locationY.toDouble(), locationZ.toDouble())
            village.setBeacon(beaconLocation)
        }
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
        villageObject.addProperty(NAME_KEY, village.name)
        villageObject.addProperty(WEALTH_KEY, village.getWealth())
        val membersArray = JsonArray()
        for (member in village.members) {
            membersArray.add(member.toString())
        }
        villageObject.add(MEMBERS_KEY, membersArray)

        val beaconLocation = village.getBeaconLocation()
        if (beaconLocation != null) {
            val beaconObject = JsonObject()
            beaconObject.addProperty(WORLD_UUID_KEY, beaconLocation.world.uid.toString())
            beaconObject.addProperty(LOCATION_X_KEY, beaconLocation.blockX)
            beaconObject.addProperty(LOCATION_Y_KEY, beaconLocation.blockY)
            beaconObject.addProperty(LOCATION_Z_KEY, beaconLocation.blockZ)
            villageObject.add(BEACON_LOCATION_KEY, beaconObject)
            for (i in 0..10) {
                JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java).logger.info("Added beacon object to JSON")
            }
        } else {
            for (i in 0..10) {
                JavaPlugin.getPlugin(GrilledCheeseLoversPlugin::class.java).logger.info("Beacon is null")
            }
        }

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