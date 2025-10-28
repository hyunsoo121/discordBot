package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface RankingHandler {
    String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    String PAGINATION_BUTTON_ID_PREFIX = "page_rank_";
    void handleRankingCommand(SlashCommandInteractionEvent event);
    void handleRankingButtonInteraction(ButtonInteractionEvent event);
}
