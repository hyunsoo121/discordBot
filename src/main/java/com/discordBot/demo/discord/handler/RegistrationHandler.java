package com.discordBot.demo.discord.handler;

import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 롤 계정 등록 관련 로직을 처리하는 핸들러입니다.
 * 이 프로젝트에서는 관리자 전용 대리 등록 로직만 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationHandler {

    private final UserService userService;
    // LolAccountRepository는 이 등록 로직의 단순화된 버전에서는 직접 사용되지 않습니다.
    // private final LolAccountRepository lolAccountRepository;

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");


    /**
     * /register 명령어를 처리합니다. (관리자 권한은 Listener에서 확인됨)
     * 이 메서드는 서버별 롤 계정 등록을 위해 discordServerId를 인자로 받습니다.
     * * @param event 슬래시 커맨드 상호작용 이벤트
     * @param discordServerId 계정이 등록될 디스코드 서버 ID
     */
    public void handleRegisterCommand(SlashCommandInteractionEvent event, Long discordServerId) {

        // 옵션 추출은 Listener에서 완료되었어야 하지만, 안전을 위해 다시 추출합니다.
        OptionMapping targetUserOption = event.getOption("target-user");
        OptionMapping nicknameOption = event.getOption("lol-nickname");

        // Listener에서 이미 검사했으므로 null 검사는 생략 가능하나, 방어적 코딩으로 유지합니다.
        if (targetUserOption == null || nicknameOption == null) {
            event.getHook().sendMessage("❌ 오류: 대상 유저와 롤 닉네임 옵션을 모두 입력해 주세요.").queue();
            return;
        }

        String fullNickname = nicknameOption.getAsString();

        // Listener에서 이미 검사했으므로, Riot ID 형식은 유효하다고 가정합니다.
        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);
        if (!matcher.matches()) {
            event.getHook().sendMessage("❌ 오류: 롤 닉네임 형식이 잘못되었습니다. '이름#태그' 형식을 사용하세요.").queue();
            return;
        }

        String gameName = matcher.group(1);
        String tagLine = matcher.group(2);
        Long targetDiscordUserId = targetUserOption.getAsUser().getIdLong();

        // deferReply는 Listener에서 이미 처리되었으므로, Hook을 사용합니다.

        try {
            // ⭐ 서버 ID를 포함하여 UserService의 등록 메서드 호출
            String resultMessage = userService.registerLolNickname(targetDiscordUserId, gameName, tagLine, discordServerId);
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("관리자 롤 계정 등록 처리 중 에러 발생 (서버 ID: {}): {}", discordServerId, e.getMessage(), e);
            // 사용자 정의 예외 메시지 전달
            event.getHook().sendMessage(e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }


    /**
     * 버튼 상호작용은 이 프로젝트의 관리자 등록 기능에서 필요 없으므로 제거되거나 다른 핸들러에서 처리되어야 합니다.
     * 이 RegistrationHandler에서는 버튼 상호작용 메서드를 제공하지 않습니다.
     */
}
