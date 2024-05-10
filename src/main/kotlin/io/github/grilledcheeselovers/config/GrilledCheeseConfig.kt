package io.github.grilledcheeselovers.config

import com.google.gson.GsonBuilder
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.util.getItemWealthAmount
import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.BoostLevelData
import io.github.grilledcheeselovers.village.BoostType
import io.github.grilledcheeselovers.village.PotionBoostData
import io.github.grilledcheeselovers.village.Upgrade
import io.github.grilledcheeselovers.village.Village
import io.github.grilledcheeselovers.village.VillageRadiusUpgrade
import io.github.grilledcheeselovers.village.VillageSpecialization
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Collections
import java.util.EnumMap
import java.util.EnumSet
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration


private const val VILLAGE_RADIUS_KEY = "village-radius"
private const val VILLAGER_CAREER_CHANGE_COOLDOWN = "villager-career-change-cooldown"

private const val WEALTH_ITEM_KEY = "wealth-item"
private const val ITEM_MATERIAL_KEY = "material"
private const val ITEM_NAME_KEY = "name"
private const val ITEM_LORE_KEY = "lore"
private const val GLOWING_KEY = "glowing"

private const val BOOSTS_KEY = "boosts"
private const val BOOST_NAME_KEY = "name"
private const val BOOST_GLOBAL_KEY = "global"
private const val BOOST_LEVELS_KEY = "levels"
private const val BOOST_TYPE_KEY = "type"
private const val BOOST_VALUE_KEY = "value"
private const val BOOST_COST_KEY = "cost"
private const val BOOST_DURATION_IN_SECONDS_KEY = "duration-in-seconds"

private const val POTION_TYPE_KEY = "potion-type"
private const val POTION_BOOST_LEVEL_KEY = "potion-level"

private const val VILLAGES_KEY = "villages"
private const val VILLAGE_MEMBERS_KEY = "members"
private const val VILLAGE_NAME_KEY = "name"
private const val VILLAGE_SPECIALIZATION_ID_KEY = "specialization"

private const val MATERIAL_VALUES_KEY = "material-values"

private const val VILLAGE_SPECIALIZATIONS_KEY = "village-specializations"
private const val VILLAGE_SPECIALIZATION_NAME_KEY = "name"
private const val VILLAGE_SPECIALIZATION_MULTIPLIER_KEY = "multiplier"
private const val VILLAGE_SPECIALIZATION_BOOSTED_MATERIALS_KEY = "boosted-materials"

private const val PLAYER_DEATH_WEALTH_LOSS_KEY = "player-death-wealth-loss"
private const val VILLAGER_DEATH_WEALTH_LOSS_KEY = "villager-death-wealth-loss"
private const val VILLAGER_CURE_WEALTH_GAIN_KEY = "villager-cure-wealth-gain"

private const val BOT_TOKEN_KEY = "bot-token"

class GrilledCheeseConfig(private val plugin: GrilledCheeseLoversPlugin) {

    private lateinit var wealthItem: ItemStack
    private var villageRadius: Int = 0
    private var possibleBoosts: MutableMap<BoostType<*>, Collection<Boost<*>>> = hashMapOf()
    private var boostsById: MutableMap<String, Boost<*>> = hashMapOf()
    private val materialValues: MutableMap<Material, Double> = EnumMap(org.bukkit.Material::class.java)
    private val specializations: MutableMap<String, VillageSpecialization> = hashMapOf()
    private var loadedVillages = false
    private var villagerCareerChangeCooldown = 0
    private val upgrades: MutableMap<String, Upgrade<*>> = hashMapOf()

    private var playerDeathWealthLoss: Double = 0.0
    private var villagerDeathWealthLoss: Double = 0.0
    private var villagerCureWealthGain: Double = 0.0

    private var botToken: String = ""

    private val villageSavesPath = this.plugin.dataFolder.toPath().resolve("villages.json")

    fun load() {
        this.plugin.saveDefaultConfig()
        val config = this.plugin.config
        this.villageRadius = config.getInt(VILLAGE_RADIUS_KEY)
        this.villagerCareerChangeCooldown = config.getInt(VILLAGER_CAREER_CHANGE_COOLDOWN)
        this.playerDeathWealthLoss = config.getDouble(PLAYER_DEATH_WEALTH_LOSS_KEY)
        this.villagerDeathWealthLoss = config.getDouble(VILLAGER_DEATH_WEALTH_LOSS_KEY)
        this.villagerCureWealthGain = config.getDouble(VILLAGER_CURE_WEALTH_GAIN_KEY)
        this.botToken = config.getString(BOT_TOKEN_KEY, "")!!
        this.wealthItem = this.loadWealthItem(config)
        this.possibleBoosts.putAll(
            this.loadBoosts(config.getConfigurationSection(BOOSTS_KEY))
        )
        this.loadVillageSpecializations(
            config.getConfigurationSection(VILLAGE_SPECIALIZATIONS_KEY)
                ?: throw IllegalArgumentException("$VILLAGE_SPECIALIZATIONS_KEY is required")
        )
        this.loadUpgrades()
        this.loadVillages(config)
        this.loadMaterialValues(
            config.getConfigurationSection(MATERIAL_VALUES_KEY)
                ?: throw IllegalArgumentException("Material values are required")
        )
    }

    fun reload() {
        this.saveVillages()
        this.plugin.villageManager.clearVillages()
        this.possibleBoosts.clear()
        this.boostsById.clear()
        this.materialValues.clear()
        this.specializations.clear()
        this.upgrades.clear()
        this.load()
    }

    fun getVillageRadius(): Int {
        return this.villageRadius
    }

    fun getVillagerCareerCooldown(): Int {
        return this.villagerCareerChangeCooldown
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

    fun getUpgradeById(id: String): Upgrade<*>? {
        return this.upgrades[id]
    }

    fun getPossibleUpgrades(): Map<String, Upgrade<*>> {
        return Collections.unmodifiableMap(this.upgrades)
    }

    fun getItemValue(itemStack: ItemStack): Double {
        val worth = getItemWealthAmount(itemStack)
        if (worth >= 1) return worth
        return this.materialValues[itemStack.type] ?: 0.0
    }

    fun getSpecializationById(id: String): VillageSpecialization? {
        return this.specializations[id]
    }

    fun getBotToken(): String {
        return this.botToken
    }

    fun getPlayerDeathWealthLoss(): Double = this.playerDeathWealthLoss
    fun getVillagerDeathWealthLoss(): Double = this.villagerDeathWealthLoss
    fun getVillagerCureWealthGain(): Double = this.villagerCureWealthGain

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
                val name = idSection.getString(VILLAGE_NAME_KEY) ?: id
                val specializationId = idSection.getString(VILLAGE_SPECIALIZATION_ID_KEY)
                    ?: throw IllegalArgumentException("$VILLAGE_SPECIALIZATION_ID_KEY is required")
                val village = Village(
                    id,
                    name,
                    this.plugin,
                    this,
                    members,
                    hashMapOf(),
                    hashMapOf(),
                    specializationId
                )
                configVillages.add(village)
            }
        }

        val lines = Files.readAllLines(this.villageSavesPath, StandardCharsets.UTF_8).joinToString(separator = "\n")
        val savedVillages = loadVillagesFromJson(
            this.plugin,
            this,
            lines
        ).toMutableMap()
        for (configVillage in configVillages) {
            savedVillages.putIfAbsent(configVillage.id, configVillage)
        }
        val villageManager = this.plugin.villageManager
        savedVillages.values.forEach {
            villageManager.addVillage(it)
            it.init()
        }
        this.loadedVillages = true
    }

    fun saveVillages() {
        if (!this.loadedVillages) return
        val villages = this.plugin.villageManager.getVillages()
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
        Files.writeString(this.villageSavesPath, string, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    private fun loadMaterialValues(section: ConfigurationSection) {
        for (key in section.getKeys(false)) {
            val material = Material.matchMaterial(key.uppercase()) ?: continue
            val value = section.getDouble(key)
            this.materialValues[material] = value
        }
    }

    private fun loadVillageSpecializations(section: ConfigurationSection) {
        for (id in section.getKeys(false)) {
            val idSection = section.getConfigurationSection(id) ?: continue
            val name = idSection.getString(VILLAGE_SPECIALIZATION_NAME_KEY) ?: id
            val multiplier = idSection.getDouble(VILLAGE_SPECIALIZATION_MULTIPLIER_KEY)
            val materials = EnumSet.noneOf(Material::class.java)
            materials.addAll(
                idSection.getStringList(VILLAGE_SPECIALIZATION_BOOSTED_MATERIALS_KEY)
                    .mapNotNull { Material.matchMaterial(it) }
                    .toSet()
            )
            this.specializations[id] = VillageSpecialization(id, name, multiplier, materials)
        }
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

                BoostType.IncreasedDurability -> {
                    loadDoubleBoost(section, id, type as BoostType<Double>, name)
                }

                BoostType.FasterCrops,
                BoostType.VillagerDiscount -> {
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
        val global = section.getBoolean(BOOST_GLOBAL_KEY, false)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, global, duration)
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
        val global = section.getBoolean(BOOST_GLOBAL_KEY, false)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, global, duration)
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
        val global = section.getBoolean(BOOST_GLOBAL_KEY, false)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, global, duration)
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
        val global = section.getBoolean(BOOST_GLOBAL_KEY, false)
        val duration = section.getInt(BOOST_DURATION_IN_SECONDS_KEY).toDuration(DurationUnit.SECONDS)
        return BoostLevelData(cost, value, global, duration)
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

    fun loadUpgrades() {
        this.upgrades[VillageRadiusUpgrade.id] = VillageRadiusUpgrade
    }

}