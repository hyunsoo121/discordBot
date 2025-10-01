package com.discordBot.demo.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

@Configuration
public class DiscordBotConfig {

    @Value("${spring.discord.bot.token}")
    private String token;

    // 2. JDA 객체를 Spring Bean으로 등록
    @Bean
    public JDA discordJDA() throws LoginException, InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("메세지 기다리는중!"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        // JDA가 완전히 로드될 때까지 기다림
         jda.awaitReady();

        return jda;
    }

    // 3. 만약 이벤트 리스너가 있다면 여기서 JDA에 추가할 수 있습니다.
    /*
    @Bean
    public void addEventListeners(JDA jda, MyEventListener listener) {
        jda.addEventListener(listener);
    }
    */
}