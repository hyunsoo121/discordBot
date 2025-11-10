package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.RegistrationHandler;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationHandlerImpl implements RegistrationHandler {

    private final UserService userService;

    private static final Map<String, String> KOREAN_LANE_MAP = Map.of(
            "탑", "TOP",
            "정글", "JUNGLE",
            "미드", "MIDDLE",
            // LineInitializer: BOTTOM - 원딜 (유의어: 바텀)
            "원딜", "BOTTOM",
            "바텀", "BOTTOM",
            // LineInitializer: SUPPORT - 서포터 (유의어: 서폿)
            "서포터", "SUPPORT",
            "서폿", "SUPPORT"
    );

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
        OptionMapping preferredLinesOption = event.getOption("preferred-lines");

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

        // ⭐⭐⭐ 선호 라인 추출, 유효성 검사 및 영문 코드 컨버팅 로직 ⭐⭐⭐
        String preferredLinesCsv;
        if (preferredLinesOption != null && StringUtils.hasText(preferredLinesOption.getAsString())) {

            String rawInput = preferredLinesOption.getAsString().toLowerCase();
            List<String> inputLines = Arrays.stream(rawInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // 유효성 검사 및 영문 코드로 변환
            List<String> validEnglishLines = inputLines.stream()
                    .map(KOREAN_LANE_MAP::get) // 한글 -> 영문 코드 매핑
                    .filter(s -> s != null) // 유효하지 않은 입력(null) 제거
                    .distinct() // 중복 제거
                    .collect(Collectors.toList());

            // 입력된 라인 중 유효하지 않은 입력이 있는 경우 오류 반환
            if (validEnglishLines.size() != inputLines.size()) {

                List<String> invalidLines = inputLines.stream()
                        .filter(line -> !KOREAN_LANE_MAP.containsKey(line))
                        .collect(Collectors.toList());

                event.getHook().sendMessage("❌ 오류: 유효하지 않은 라인 입력이 포함되어 있습니다. 확인된 오류: " +
                                String.join(", ", invalidLines) +
                                " (유효한 라인: 탑, 정글, 미드, 원딜/바텀, 서폿/서포터)")
                        .queue();
                return;
            }

            preferredLinesCsv = String.join(",", validEnglishLines);

        } else {
            preferredLinesCsv = ""; // 선호 라인 미입력
        }
        // ⭐⭐⭐ 로직 끝 ⭐⭐⭐

        Long targetDiscordUserId = targetUserOption.getAsUser().getIdLong();
        Long discordServerId = event.getGuild().getIdLong();

        try {
            String resultMessage = userService.registerLolNickname(
                    targetDiscordUserId,
                    gameName,
                    tagLine,
                    discordServerId,
                    preferredLinesCsv // 영문 코드 CSV 전달 (예: "TOP,JUNGLE")
            );
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("관리자 롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage(e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }
}