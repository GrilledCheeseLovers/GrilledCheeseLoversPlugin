package io.github.grilledcheeselovers.config

import com.google.gson.GsonBuilder
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.BoostLevelData
import io.github.grilledcheeselovers.village.BoostType
import io.github.grilledcheeselovers.village.PotionBoostData
import io.github.grilledcheeselovers.village.Village
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration


private const val VILLAGE_RADIUS_KEY = "village-radius"

private const val WEALTH_ITEM_KEY = "wealth-item"
private const val ITEM_MATERIAL_KEY = "material"
private const val ITEM_NAME_KEY = "name"
private const val ITEM_LORE_KEY = "lore"
private const val GLOWING_KEY = "glowing"

private const val BOOSTS_KEY = "boosts"
private const val BOOST_NAME_KEY = "name"
private const val BOOST_LEVELS_KEY = "levels"
private const val BOOST_TYPE_KEY = "type"
private const val BOOST_VALUE_KEY = "value"
private const val BOOST_COST_KEY = "cost"
private const val BOOST_DURATION_IN_SECONDS_KEY = "duration-in-seconds"

private const val POTION_TYPE_KEY = "potion-type"
private const val POTION_BOOST_LEVEL_KEY = "potion-level"

private const val VILLAGES_KEY = "villages"
private const val VILLAGE_MEMBERS_KEY = "members"

class GrilledCheeseConfig(private val plugin: GrilledCheeseLoversPlugin) {

    private lateinit var wealthItem: ItemStack
    private var villageRadius: Int = 0
    private var possibleBoosts: MutableMap<BoostType<*>, Collection<Boost<*>>> = hashMapOf()
    private var boostsById: MutableMap<String, Boost<*>> = hashMapOf()

    private val villageSavesPath = this.plugin.dataFolder.toPath().resolve("villages.json")

    fun load() {
        this.plugin.saveDefaultConfig()
        val config = this.plugin.config
        this.villageRadius = config.getInt(VILLAGE_RADIUS_KEY)
        this.wealthItem = this.loadWealthItem(config)
        this.possibleBoosts.putAll(
            this.loadBoosts(config.getConfigurationSection(BOOSTS_KEY))
        )
        this.loadVillages(config)
    }

    fun getVillageRadius(): Int {
        return this.villageRadius
    }

    fun getWealthItem(): ItemStack {
        return this.wealthItem.clone()
    }

    fun getPossibleBoosts(): Map<BoostType<*>, Collection<Boost<*>>> {
        return this.possibleBoosts
    }

    fun getBoostById(id: String): Boost<*>? {
        return this.boostsById[id]
    }

    private fun loadVillages(config: FileConfiguration) {
        val villageSaveFile = this.villageSavesPath.toFile()
        if (!villageSaveFile.exists()) {
            villageSaveFile.parentFile.mkdirs()
            villageSaveFile.createNewFile()
        }

        val villagesSection = config.getConfigurationSection(VILLAGES_KEY)
        val configVillages: MutableList<Village> = arrayListOf()
        if (villagesSection != null) {
            for (id in villagesSection.getKeys(false)) {
                val idSection = villagesSection.getConfigurationSection(id) ?: continue
                val members =
                    idSection.getStringList(VILLAGE_MEMBERS_KEY).map { UUID.fromString(it) }
                        .toSet()
                val village = Village(
                    id,
                    this.plugin,
                    this,
                    members,
                    hashMapOf(),
                    hashMapOf()
                )
                configVillages.add(village)
            }
        }

        val savedVillages = loadVillagesFromJson(
            this.plugin,
            this,
            Files.readAllLines(this.villageSavesPath, StandardCharsets.UTF_8).joinToString { "\n" })
            .toMutableMap()
        for (configVillage in configVillages) {
            savedVillages.putIfAbsent(configVillage.id, configVillage)
        }
        val villageManager = this.plugin.villageManger
        savedVillages.values.forEach { villageManager.addVillage(it) }
    }

    fun saveVillages() {
        val villages = this.plugin.villageManger.getVillages()
        val villageSaveFile = villageSavesPath.toFile()
        if (!villageSaveFile.exists()) {
            villageSaveFile.parentFile.mkdirs()
            villageSaveFile.createNewFile()
        }
        val json = convertVillagesToJson(villages)
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        val string = gson.toJson(json)
        Files.writeString(this.villageSavesPath, string, StandardOpenOption.WRITE)
    }

    private fun loadBoosts(boostsSection: ConfigurationSection?): Map<BoostType<*>, Collection<Boost<*>>> {
        if (boostsSection == null) return emptyMap()
        val boosts: MutableMap<BoostType<*>, Collection<Boost<*>>> = hashMapOf()
        for (id in boostsSection.getKeys(false)) {
            val section = boostsSection.getConfigurationSection(id)
                ?: throw IllegalArgumentException("$id is not a valid boost")
            val type = BoostType.getBoostType(
                section.getString(BOOST_TYPE_KEY)
                    ?: throw IllegalArgumentException("$BOOST_TYPE_KEY is required")
            ) ?: throw IllegalArgumentException("${section.getString(BOOST_TYPE_KEY)} was not found")

            val name = section.getString(BOOST_NAME_KEY) ?: id

            val boost: Boost<*> = when (type) {
                BoostType.NoPhantoms -> {
                    loadBooleanBoost(section, id, type as BoostType<Boolean>, name)
                }

                BoostType.PotionEffect -> {
                    loadPotionBoost(section, id, type as BoostType<PotionBoostData>, name)
                }

                BoostType.FasterCrops,
                BoostType.VillagerDiscount,
                BoostType.IncreasedDurability -> {
                    loadDoubleBoost(section, id, type as BoostType<Double>, name)
                }

                BoostType.VillageRadius -> {
                    loadIntBoost(section, id, type as BoostType<Int>, name)
                }
            }
            val boostList = boosts.computeIfAbsent(type) { _ -> arrayListOf() } as MutableList<Boost<*>>
            boostList.add(boost)
            this.boostsById[boost.id] = boost
        }
        return boosts
    }

    private fun <T> loadLevelData(
        levelsSection: ConfigurationSection,
        loader: (ConfigurationSection) -> BoostLevelData<T>
    ): Map<Int, BoostLevelData<T>> {
        val levels: MutableMap<Int, BoostLevelData<T>> = hashMapOf()
        for (levelStr in levelsSection.getKeys(false)) {
            val levelSection = levelsSection.getConfigurationSection(levelStr)
                ?: throw IllegalArgumentException("$levelStr does not have valid level data")
            val level = levelStr.toInt()
            val data = loader(levelSection)
            levels[level] = data
        }
        return levels
    }

    private fun loadDoubleBoostValue(
        section: ConfigurationSection
    ): BoostLevelData<Double> {
        val value = section.getDouble(BOOST_VALUE_KEY)
        val cost = section.getDouble(BOOST_COST_KEY)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, duration)
    }

    private fun loadIntBoost(
        section: ConfigurationSection,
        id: String,
        type: BoostType<Int>,
        name: String
    ): Boost<Int> {
        val levelsSection = section.getConfigurationSection(BOOST_LEVELS_KEY)
            ?: throw IllegalArgumentException("$BOOST_LEVELS_KEY is required")
        val levels = loadLevelData(
            levelsSection
        ) { loadIntBoostValue(it) }
        return Boost(id, name, type, levels)
    }

    private fun loadIntBoostValue(
        section: ConfigurationSection
    ): BoostLevelData<Int> {
        val value = section.getInt(BOOST_VALUE_KEY)
        val cost = section.getDouble(BOOST_COST_KEY)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, duration)
    }

    private fun loadDoubleBoost(
        section: ConfigurationSection,
        id: String,
        type: BoostType<Double>,
        name: String
    ): Boost<Double> {
        val levelsSection = section.getConfigurationSection(BOOST_LEVELS_KEY)
            ?: throw IllegalArgumentException("$BOOST_LEVELS_KEY is required")
        val levels = loadLevelData(
            levelsSection
        ) { loadDoubleBoostValue(it) }
        return Boost(id, name, type, levels)
    }


    private fun loadBooleanBoostValue(
        section: ConfigurationSection
    ): BoostLevelData<Boolean> {
        val value = section.getBoolean(BOOST_VALUE_KEY)
        val cost = section.getDouble(BOOST_COST_KEY)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, duration)
    }

    private fun loadBooleanBoost(
        section: ConfigurationSection,
        id: String,
        type: BoostType<Boolean>,
        name: String
    ): Boost<Boolean> {
        val levelsSection = section.getConfigurationSection(BOOST_LEVELS_KEY)
            ?: throw IllegalArgumentException("$BOOST_LEVELS_KEY is required")
        val levels = loadLevelData(
            levelsSection
        ) { loadBooleanBoostValue(it) }
        return Boost(id, name, type, levels)
    }


    private fun loadPotionBoostDataValue(
        section: ConfigurationSection,
    ): BoostLevelData<PotionBoostData> {
        val value = loadPotionBoostData(section)
        val cost = section.getDouble(BOOST_COST_KEY)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, duration)
    }

    private fun loadPotionBoostData(section: ConfigurationSection): PotionBoostData {
        val typeString = section.getString(POTION_TYPE_KEY)
            ?: throw IllegalArgumentException("$POTION_TYPE_KEY is required for potion boosts")
        val effectType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(typeString))
            ?: throw IllegalArgumentException("Potion effect type not found: $typeString")
        val level = section.getInt(POTION_BOOST_LEVEL_KEY)
        return PotionBoostData(effectType, level)
    }

    private fun loadPotionBoost(
        section: ConfigurationSection,
        id: String,
        type: BoostType<PotionBoostData>,
        name: String
    ): Boost<PotionBoostData> {
        val levelsSection = section.getConfigurationSection(BOOST_LEVELS_KEY)
            ?: throw IllegalArgumentException("$BOOST_LEVELS_KEY is required")
        val levels = loadLevelData(
            levelsSection
        ) { loadPotionBoostDataValue(it) }
        return Boost(id, name, type, levels)
    }

    private fun loadWealthItem(config: FileConfiguration): ItemStack {
        val section = config.getConfigurationSection(WEALTH_ITEM_KEY)
            ?: throw IllegalArgumentException("$WEALTH_ITEM_KEY is required")
        return this.loadItem(section)
    }

    private fun loadItem(section: ConfigurationSection): ItemStack {
        val material = Material.matchMaterial(
            section.getString(ITEM_MATERIAL_KEY) ?: throw IllegalArgumentException("Item type required")
        ) ?: throw IllegalStateException("Invalid item: ${section.getString(ITEM_MATERIAL_KEY)}")
        val itemStack = ItemStack(material)
        val meta = itemStack.itemMeta ?: return itemStack
        section.getString(ITEM_NAME_KEY)?.apply { meta.displayName(MINI_MESSAGE.deserialize(this)) }
        meta.lore(section.getStringList(ITEM_LORE_KEY).map {
            MINI_MESSAGE.deserialize(it)
        })
        if (section.getBoolean(GLOWING_KEY, false)) {
            val flags = itemStack.itemFlags.toMutableList()
            flags.add(ItemFlag.HIDE_ENCHANTS)
            itemStack.addUnsafeEnchantment(Enchantment.LURE, 1)
        }
        itemStack.itemMeta = meta
        return itemStack
    }


}