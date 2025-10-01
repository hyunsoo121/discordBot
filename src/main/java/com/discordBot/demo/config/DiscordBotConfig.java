package com.discordBot.demo.config;

import com.discordBot.demo.listener.DiscordBotListener;
import com.discordBot.demo.listener.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Value("${spring.discord.bot.token}")
    private String token;

    @Bean
    public JDA discordJDA() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("메세지 기다리는중!"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        new DiscordBotListener(),
                        new SlashCommandListener()
                )
                .build();

         jda.awaitReady();

        return jda;
    }
}