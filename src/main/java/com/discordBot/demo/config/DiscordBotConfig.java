package com.discordBot.demo.config;

import com.discordBot.demo.listener.DiscordBotListener;
import com.discordBot.demo.listener.SlashCommandListener;
import lombok.RequiredArgsConstructor; // 💡 추가: final 필드 자동 주입
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DiscordBotConfig {

    private final DiscordBotListener discordBotListener;
    private final SlashCommandListener slashCommandListener;

    @Value("${spring.discord.bot.token}")
    private String token;

    @Bean
    public JDA discordJDA() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("메세지 기다리는중!"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        discordBotListener,
                        slashCommandListener
                )
                .build();

        jda.awaitReady();

        return jda;
    }
}