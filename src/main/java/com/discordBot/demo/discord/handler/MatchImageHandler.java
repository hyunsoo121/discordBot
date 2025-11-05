package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface MatchImageHandler {

    String BUTTON_ID_CONFIRM = "match-confirm";
    String BUTTON_ID_CANCEL = "match-cancel";
    void handleMatchUploadCommand(SlashCommandInteractionEvent event);
    void handleFinalConfirmation(ButtonInteractionEvent event);
}
