package io.github.grilledcheeselovers.village.discord

import io.github.grilledcheeselovers.GrilledCheeseLoversPlugin
import io.github.grilledcheeselovers.constant.MINI_MESSAGE
import io.github.grilledcheeselovers.village.VillageManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import java.awt.Color
import java.util.EnumSet

private const val VILLAGE_COMMAND_NAME = "village"
private const val VILLAGE_COMMAND_LEADERBOARD = "leaderboard"

class VillageBot(
    private val token: String,
    private val plugin: GrilledCheeseLoversPlugin,
    private val villageManager: VillageManager = plugin.villageManager
) {

    private val bot =
        JDABuilder.createLight(
            this.token,
            EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        )
            .addEventListeners(VillageCommandListener(this.villageManager))
            .build();

    fun initializeCommands() {
        val commands = bot.updateCommands()
        commands.addCommands(
            Commands.slash(VILLAGE_COMMAND_NAME, "Makes the bot say what you tell it to")
                .addSubcommands(SubcommandData(VILLAGE_COMMAND_LEADERBOARD, "Leaderboard of village wealth"))
        )
        commands.queue()
    }

}

private class VillageCommandListener(private val villageManager: VillageManager) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            VILLAGE_COMMAND_LEADERBOARD -> {
                event.channel.sendMessageEmbeds(buildLeaderboardEmbed()).queue()
                event.reply("Success").setEphemeral(true).queue()
            }
        }
    }

    private fun buildLeaderboardEmbed(): MessageEmbed {
        val builder = EmbedBuilder()
        builder.setTitle("Villages Leaderboard")
        builder.setColor(Color(199, 251, 255))

        val villages = this.villageManager.getVillages().values.sortedByDescending { it.getWealth() }.toList()

        var place = 1
        for (village in villages) {
            builder.addField("", "${place}: ${MINI_MESSAGE.stripTags(village.name)} (${village.getWealth()} wealth)", false)
//            builder.addBlankField(false)
            place++
        }
        return builder.build()
    }
}