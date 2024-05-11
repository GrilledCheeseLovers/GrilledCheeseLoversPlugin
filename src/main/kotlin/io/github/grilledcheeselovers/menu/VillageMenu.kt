package io.github.grilledcheeselovers.menu

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.config.GrilledCheeseConfig
import io.github.grilledcheeselovers.constant.ACTIVE_BOOSTS_MENU_NAME
import io.github.grilledcheeselovers.constant.ACTIVE_BOOSTS_MENU_OPTION_ITEM_NAME
import io.github.grilledcheeselovers.constant.BOOST_MENU_OPTION_ITEM_NAME
import io.github.grilledcheeselovers.constant.BOOST_TYPES_MENU_NAME
import io.github.grilledcheeselovers.constant.MATERIAL_DEPOSIT_MENU_NAME
import io.github.grilledcheeselovers.constant.MATERIAL_DEPOSIT_MENU_OPTION_ITEM_NAME
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.constant.NOT_IN_VILLAGE
import io.github.grilledcheeselovers.constant.UPGRADES_MENU_NAME
import io.github.grilledcheeselovers.constant.UPGRADES_MENU_OPTION_ITEM_NAME
import io.github.grilledcheeselovers.constant.getBoostLevelsMenuName
import io.github.grilledcheeselovers.constant.getBoostTypeMenuName
import io.github.grilledcheeselovers.constant.getMainMenuName
import io.github.grilledcheeselovers.extension.getVillage
import io.github.grilledcheeselovers.util.formatDuration
import io.github.grilledcheeselovers.village.ActiveBoost
import io.github.grilledcheeselovers.village.Boost
import io.github.grilledcheeselovers.village.BoostLevelData
import io.github.grilledcheeselovers.village.BoostType
import io.github.grilledcheeselovers.village.Village
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.LocalDateTime
import java.util.function.Consumer
import kotlin.time.toJavaDuration

private const val MAIN_MENU_ROWS = 6
private const val MAIN_MENU_OPTIONS_START_X = 1
private const val MAIN_MENU_OPTIONS_START_Y = 1
private const val MAIN_MENU_OPTIONS_WIDTH = 7
private const val MAIN_MENU_OPTIONS_HEIGHT = 4

private const val BOOST_OPTIONS_MENU_ROWS = 6
private const val BOOST_OPTIONS_MENU_OPTIONS_START_X = 1
private const val BOOST_OPTIONS_MENU_OPTIONS_START_Y = 1
private const val BOOST_OPTIONS_MENU_OPTIONS_WIDTH = 7
private const val BOOST_OPTIONS_MENU_OPTIONS_HEIGHT = 4

private const val BOOST_MENU_ROWS = 6
private const val BOOST_MENU_OPTIONS_START_X = 1
private const val BOOST_MENU_OPTIONS_START_Y = 1
private const val BOOST_MENU_OPTIONS_WIDTH = 7
private const val BOOST_MENU_OPTIONS_HEIGHT = 4

private const val BOOST_LEVELS_MENU_ROWS = 6
private const val BOOST_LEVELS_MENU_OPTIONS_START_X = 1
private const val BOOST_LEVELS_MENU_OPTIONS_START_Y = 1
private const val BOOST_LEVELS_MENU_OPTIONS_WIDTH = 7
private const val BOOST_LEVELS_MENU_OPTIONS_HEIGHT = 4

private const val DEPOSIT_MATERIALS_MENU_ROWS = 4

private const val ACTIVE_BOOSTS_MENU_ROWS = 6
private const val ACTIVE_BOOSTS_MENU_START_X = 1
private const val ACTIVE_BOOSTS_MENU_START_Y = 1
private const val ACTIVE_BOOSTS_MENU_WIDTH = 7
private const val ACTIVE_BOOSTS_MENU_HEIGHT = 4

private const val UPGRADES_MENU_ROWS = 6
private const val UPGRADES_MENU_START_X = 1
private const val UPGRADES_MENU_START_Y = 1
private const val UPGRADES_MENU_WIDTH = 7
private const val UPGRADES_MENU_HEIGHT = 4

fun sendMainMenu(player: Player, plugin: GrilledCheeseLoversPlugin) {
    val village = player.getVillage(plugin.villageManager)
    if (village == null) {
        player.sendMessage(NOT_IN_VILLAGE)
        return
    }
    createMainMenu(village, plugin).show(player)
}

private fun createMainMenu(village: Village, plugin: GrilledCheeseLoversPlugin): Gui {
    val gui = ChestGui(MAIN_MENU_ROWS, ComponentHolder.of(getMainMenuName(village)))
    val menuOptionsPane = OutlinePane(
        MAIN_MENU_OPTIONS_START_X,
        MAIN_MENU_OPTIONS_START_Y,
        MAIN_MENU_OPTIONS_WIDTH,
        MAIN_MENU_OPTIONS_HEIGHT
    )
    menuOptionsPane.addItem(createBoostMenuOptionItem(plugin))
    menuOptionsPane.addItem(createWealthDepositMenuOptionItem(plugin))
    menuOptionsPane.addItem(createActiveBoostsMenuOptionItem(plugin))
    menuOptionsPane.addItem(createUpgradesMenuOptionItem(plugin))
    gui.addPane(menuOptionsPane)
    disableClicks(gui)
    return gui
}

private fun createBoostMenuOptionItem(plugin: GrilledCheeseLoversPlugin): GuiItem {
    val itemStack = ItemStack(Material.EXPERIENCE_BOTTLE)
    val meta = itemStack.itemMeta!!
    meta.displayName(BOOST_MENU_OPTION_ITEM_NAME)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openBoostOptionsMenu(player, plugin)
    })
    return guiItem
}

private fun createWealthDepositMenuOptionItem(plugin: GrilledCheeseLoversPlugin): GuiItem {
    val itemStack = ItemStack(Material.CAULDRON)
    val meta = itemStack.itemMeta!!
    meta.displayName(MATERIAL_DEPOSIT_MENU_OPTION_ITEM_NAME)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openMaterialDepositMenu(plugin, player)
    })
    return guiItem
}

private fun createActiveBoostsMenuOptionItem(plugin: GrilledCheeseLoversPlugin): GuiItem {
    val itemStack = ItemStack(Material.REDSTONE_LAMP)
    val meta = itemStack.itemMeta!!
    meta.displayName(ACTIVE_BOOSTS_MENU_OPTION_ITEM_NAME)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openActiveBoostsMenu(plugin, player)
    })
    return guiItem
}

private fun createUpgradesMenuOptionItem(plugin: GrilledCheeseLoversPlugin): GuiItem {
    val itemStack = ItemStack(Material.ANVIL)
    val meta = itemStack.itemMeta!!
    meta.displayName(UPGRADES_MENU_OPTION_ITEM_NAME)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openUpgradesMenu(plugin, player)
    })
    return guiItem
}


private fun openBoostOptionsMenu(player: Player, plugin: GrilledCheeseLoversPlugin) {
    val config = plugin.grilledCheeseConfig
    val possibleBoosts = config.getPossibleBoosts()
    val gui = ChestGui(BOOST_OPTIONS_MENU_ROWS, ComponentHolder.of(BOOST_TYPES_MENU_NAME))
    val pane = OutlinePane(
        BOOST_OPTIONS_MENU_OPTIONS_START_X,
        BOOST_OPTIONS_MENU_OPTIONS_START_Y,
        BOOST_OPTIONS_MENU_OPTIONS_WIDTH,
        BOOST_OPTIONS_MENU_OPTIONS_HEIGHT
    )
    for (entry in possibleBoosts) {
        pane.addItem(createBoostTypeMenuItem(plugin, entry.key, entry.value))
    }
    gui.addPane(pane)
    disableClicks(gui)
    gui.show(player)
}

private fun createBoostTypeMenuItem(
    plugin: GrilledCheeseLoversPlugin,
    boostType: BoostType<*>,
    boosts: Collection<Boost<*>>
): GuiItem {
    val itemStack = ItemStack(Material.BEACON)
    val meta = itemStack.itemMeta!!
    meta.displayName(MINI_MESSAGE.deserialize(boostType.displayName))
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openBoostTypeMenu(plugin, player, boostType, boosts)
    })
    return guiItem
}

private fun openBoostTypeMenu(
    plugin: GrilledCheeseLoversPlugin,
    player: Player,
    boostType: BoostType<*>,
    boosts: Collection<Boost<*>>
) {
    val gui = ChestGui(BOOST_MENU_ROWS, ComponentHolder.of(getBoostTypeMenuName(boostType)))
    val pane = OutlinePane(
        BOOST_MENU_OPTIONS_START_X,
        BOOST_MENU_OPTIONS_START_Y,
        BOOST_MENU_OPTIONS_WIDTH,
        BOOST_MENU_OPTIONS_HEIGHT
    )
    for (boost in boosts) {
        pane.addItem(createBoostMenuItem(plugin, boost))
    }
    gui.addPane(pane)
    disableClicks(gui)
    gui.show(player)
}

private fun createBoostMenuItem(plugin: GrilledCheeseLoversPlugin, boost: Boost<*>): GuiItem {
    val itemStack = ItemStack(Material.REDSTONE_LAMP)
    val meta = itemStack.itemMeta!!
    meta.displayName(MINI_MESSAGE.deserialize(boost.name))
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        openBoostLevelsMenu(plugin, boost, player)
    })
    return guiItem
}

private fun openBoostLevelsMenu(plugin: GrilledCheeseLoversPlugin, boost: Boost<*>, player: Player) {
    val gui = ChestGui(BOOST_LEVELS_MENU_ROWS, ComponentHolder.of(getBoostLevelsMenuName(boost)))
    val pane = OutlinePane(
        BOOST_LEVELS_MENU_OPTIONS_START_X,
        BOOST_LEVELS_MENU_OPTIONS_START_Y,
        BOOST_LEVELS_MENU_OPTIONS_WIDTH,
        BOOST_LEVELS_MENU_OPTIONS_HEIGHT
    )
    for (entry in boost.levelValues) {
        pane.addItem(createBoostLevelItem(plugin, boost, entry.key, entry.value))
    }
    gui.addPane(pane)
    disableClicks(gui)
    gui.show(player)
}

private fun createBoostLevelItem(
    plugin: GrilledCheeseLoversPlugin,
    boost: Boost<*>,
    level: Int,
    levelData: BoostLevelData<*>
): GuiItem {
    val itemStack = ItemStack(Material.REDSTONE_LAMP)
    val meta = itemStack.itemMeta!!
    meta.displayName(MINI_MESSAGE.deserialize(boost.name))
    val lore = mutableListOf(
        Component.empty(),
        MINI_MESSAGE.deserialize("global: ${if (levelData.global) "<green>true" else "<red>false"}"),
        MINI_MESSAGE.deserialize("Duration: ${formatDuration(levelData.duration.toJavaDuration())}"),
        MINI_MESSAGE.deserialize("<aqua>Level: $level"),
        MINI_MESSAGE.deserialize("<aqua>Price: ${levelData.cost}")
    )
    meta.lore(lore)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        val village = player.getVillage(plugin.villageManager) ?: return@Consumer
        village.attemptPurchaseBoost(player, boost, level)
    })
    return guiItem
}

private fun openMaterialDepositMenu(
    plugin: GrilledCheeseLoversPlugin,
    player: Player
) {
    val gui = ChestGui(DEPOSIT_MATERIALS_MENU_ROWS, ComponentHolder.of(MATERIAL_DEPOSIT_MENU_NAME))
    val village = player.getVillage(plugin.villageManager) ?: return
    val specialization = plugin.grilledCheeseConfig.getSpecializationById(village.getSpecialization())
    gui.setOnClose { event ->
        val inventory = event.view.topInventory
        for (item in inventory) {
            if (item == null || item.type.isAir) continue
            var specializationMultiplier = 1.0
            if (specialization != null && specialization.boostedMaterials.contains(item.type)) {
                specializationMultiplier = specialization.multiplier
            }
            val worth = plugin.grilledCheeseConfig.getItemValue(item) * specializationMultiplier
            if (worth == 0.0) {
                player.inventory.addItem(item)
                continue
            }
            village.depositWealth(player, worth * item.amount, item, item.amount)
        }
    }
    gui.show(player)
}

private fun openActiveBoostsMenu(
    plugin: GrilledCheeseLoversPlugin,
    player: Player
) {
    val gui = ChestGui(ACTIVE_BOOSTS_MENU_ROWS, ComponentHolder.of(ACTIVE_BOOSTS_MENU_NAME))
    val village = player.getVillage(plugin.villageManager) ?: return
    val pane = OutlinePane(
        ACTIVE_BOOSTS_MENU_START_X,
        ACTIVE_BOOSTS_MENU_START_Y,
        ACTIVE_BOOSTS_MENU_WIDTH,
        ACTIVE_BOOSTS_MENU_HEIGHT
    )
    for (boost in village.getActiveBoosts().values) {
        val guiItem = getActiveBoostItem(plugin.grilledCheeseConfig, boost) ?: continue
        pane.addItem(guiItem)
    }
    gui.addPane(pane)
    disableClicks(gui)
    gui.show(player)
}

private fun getActiveBoostItem(
    config: GrilledCheeseConfig,
    activeBoost: ActiveBoost<*>
): GuiItem? {
    val boost = config.getBoostById(activeBoost.boostId) ?: return null
    val itemStack = ItemStack(Material.REDSTONE_LAMP)
    val meta = itemStack.itemMeta!!
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    meta.addEnchant(Enchantment.OXYGEN, 0, true)
    meta.displayName(MINI_MESSAGE.deserialize(boost.name))
    val timeLeft = Duration.between(LocalDateTime.now(), activeBoost.endTime)
    val lore = mutableListOf(
        Component.empty(),
        MINI_MESSAGE.deserialize("<aqua>Time left: ${formatDuration(timeLeft)}")
    )
    meta.lore(lore)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack)
    return guiItem
}

private fun openUpgradesMenu(
    plugin: GrilledCheeseLoversPlugin,
    player: Player
) {
    val gui = ChestGui(UPGRADES_MENU_ROWS, ComponentHolder.of(UPGRADES_MENU_NAME))
    val village = player.getVillage(plugin.villageManager) ?: return
    val pane = OutlinePane(
        UPGRADES_MENU_START_X,
        UPGRADES_MENU_START_Y,
        UPGRADES_MENU_WIDTH,
        UPGRADES_MENU_HEIGHT
    )
    for (entry in plugin.grilledCheeseConfig.getPossibleUpgrades()) {
        val id = entry.key
        val upgradeLevel = village.getUpgradeLevels()[id] ?: 0
        val upgradeItem = getUpgradeItem(plugin, entry.key, upgradeLevel) ?: continue
        pane.addItem(upgradeItem)
    }
    gui.addPane(pane)
    disableClicks(gui)
    gui.show(player)
}

private fun getUpgradeItem(
    plugin: GrilledCheeseLoversPlugin,
    upgradeId: String,
    upgradeLevel: Int
): GuiItem? {
    val upgrade = plugin.grilledCheeseConfig.getUpgradeById(upgradeId) ?: return null
    val itemStack = ItemStack(Material.ANVIL)
    val meta = itemStack.itemMeta!!
    meta.displayName(MINI_MESSAGE.deserialize(upgrade.name))
    val cost = upgrade.costCalculator(upgradeLevel + 1)
    val currentValue = upgrade.upgradeCalculator(upgradeLevel)
    val lore = mutableListOf(
        Component.empty(),
        MINI_MESSAGE.deserialize("<aqua>Current value: $currentValue"),
        MINI_MESSAGE.deserialize("<aqua>Cost for next level: $cost")
    )
    meta.lore(lore)
    itemStack.itemMeta = meta
    val guiItem = GuiItem(itemStack, Consumer { event ->
        val player = event.whoClicked as? Player ?: return@Consumer
        val village = player.getVillage(plugin.villageManager) ?: return@Consumer
        village.attemptPurchaseUpgrade(player, upgradeId)
        openUpgradesMenu(plugin, player)
    })
    return guiItem
}

private fun disableClicks(gui: Gui) {
    gui.setOnGlobalClick { event -> event.isCancelled = true }
    gui.setOnGlobalDrag { event -> event.isCancelled = true }
}
