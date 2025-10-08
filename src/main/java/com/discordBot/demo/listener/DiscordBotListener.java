package com.discordBot.demo.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordBotListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User user = event.getAuthor();
        TextChannel textChannel = event.getChannel().asTextChannel();
        Message message = event.getMessage();

        log.info(" get message :" + message.getContentDisplay());

        if (user.isBot()){
            return;
        }
        else if (message.getContentDisplay().equals("")){
            log.info("디스코드 message 문자열 값 공백");
        }

        String[] messageArray = message.getContentDisplay().split(" ");

        if (messageArray[0].equalsIgnoreCase("봇아")) {


            String[] messageArgs = Arrays.copyOfRange(messageArray, 1, messageArray.length);

            for (String msg : messageArgs){
                String returnMessage = sendMessage(event, msg);
                textChannel.sendMessage(returnMessage).queue();
            }
        }

    }

    private String sendMessage(MessageReceivedEvent event, String message){
        User user = event.getAuthor();
        String returnMessage;

        switch (message){
            case "test" : returnMessage = user.getName() + "테스트 중이세요??";
            break;
            case "안녕" : returnMessage = user.getName() + "님 안녕하세요!";
            break;
            case "누구야" : returnMessage = user.getAsMention() + "님 저는 뭐 네.";
            break;
            default: returnMessage = "등록되지 않은 명령어입니다.";
            break;
        }

        return returnMessage;
    }
}
