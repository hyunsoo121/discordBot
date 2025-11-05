package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;


public interface RegistrationHandler {
    void handleRegisterCommand(SlashCommandInteractionEvent event);

}
