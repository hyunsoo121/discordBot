package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.RegistrationHandler;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationHandlerImpl implements RegistrationHandler {

    private final UserService userService;

    public void handleRegisterCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue();

        Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessage("❌ 오류: 이 명령어는 **서버 관리자**만 사용할 수 있습니다.").queue();
            return;
        }

        OptionMapping targetUserOption = event.getOption("target-user");
        OptionMapping nicknameOption = event.getOption("lol-nickname");

        if (targetUserOption == null || nicknameOption == null) {
            event.getHook().sendMessage("❌ 오류: 대상 유저와 롤 닉네임 옵션을 모두 입력해 주세요.").queue();
            return;
        }

        String fullNickname = nicknameOption.getAsString();
        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);

        if (!matcher.matches()) {
            event.getHook().sendMessage("❌ 오류: 롤 닉네임을 **'게임이름#태그'** 형식으로 정확히 입력해 주세요. (예: Faker#KR1)").queue();
            return;
        }

        String gameName = matcher.group(1);
        String tagLine = matcher.group(2);

        Long targetDiscordUserId = targetUserOption.getAsUser().getIdLong();
        Long discordServerId = event.getGuild().getIdLong();

        try {
            String resultMessage = userService.registerLolNickname(targetDiscordUserId, gameName, tagLine, discordServerId);
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("관리자 롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage(e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }
}
