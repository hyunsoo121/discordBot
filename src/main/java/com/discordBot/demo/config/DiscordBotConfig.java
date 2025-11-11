package com.discordBot.demo.config;

import com.discordBot.demo.discord.listener.*;
import lombok.RequiredArgsConstructor;
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

    private final SlashCommandListener slashCommandListener;
    private final RankingButtonListener rankingButtonListener;
    private final MatchButtonListener matchButtonListener;
    private final UserButtonListener userButtonListener;
    private final MatchModalListener matchModalListener;
    private final InteractionDebugListener interactionDebugListener;

    @Value("${spring.discord.bot.token}")
    private String token;

    @Bean
    public JDA discordJDA() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("내전 기록을 보관하는 중"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        slashCommandListener,
                        rankingButtonListener,
                        matchButtonListener,
                        userButtonListener,
                        matchModalListener
//                        interactionDebugListener
                )
                .build();

        jda.awaitReady();

        return jda;
    }
}