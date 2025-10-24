package com.discordBot.demo.config;

import com.discordBot.demo.discord.listener.RankingButtonListener;
import com.discordBot.demo.discord.listener.SlashCommandListener;
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
    @Value("${spring.discord.bot.token}")
    private String token;

    @Bean
    public JDA discordJDA() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("내전 기록 확인"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        slashCommandListener, // Slash 명령어 처리
                        rankingButtonListener // 버튼 이벤트 리스너 등록
                )
                .build();

        jda.awaitReady();

        return jda;
    }
}