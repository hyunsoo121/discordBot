package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface RankingHandler {
    void handleRankingCommand(SlashCommandInteractionEvent event);
}
