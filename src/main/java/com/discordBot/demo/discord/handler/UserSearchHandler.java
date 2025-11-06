package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface UserSearchHandler {
    void    handleUserStatsCommand(SlashCommandInteractionEvent event);
    void handlePagination(ButtonInteractionEvent event);
}