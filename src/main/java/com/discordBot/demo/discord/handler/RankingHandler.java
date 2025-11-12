package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface RankingHandler {
    String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    String PAGINATION_BUTTON_ID_PREFIX = "page_rank_";
    String SORT_LINE_BUTTON_ID_PREFIX = "sort_line_rank_";
    String PAGINATION_LINE_BUTTON_ID_PREFIX = "page_line_rank_";
    void handleRankingCommand(SlashCommandInteractionEvent event);
    void handleLineRankingCommand(SlashCommandInteractionEvent event);
    void handleRankingButtonInteraction(ButtonInteractionEvent event);
}
