package com.discordBot.demo.discord.handler; // discord 패키지로 분리

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.discord.component.MatchModalBuilder;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.MatchSessionManager;
import com.discordBot.demo.service.MatchSessionManager.MatchSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.discordBot.demo.discord.component.MatchModalBuilder.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchInteractionHandler {

    private final MatchSessionManager sessionManager;
    private final MatchRecordService matchRecordService;
    private final MatchModalBuilder modalBuilder;

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");


    /**
     * /match-register 슬래시 명령어를 처리하고 첫 번째 모달을 띄웁니다.
     */
    public void handleMatchRegisterCommand(SlashCommandInteractionEvent event) {
        String initiatorId = event.getUser().getId();

        String winnerTeam = event.getOption("winner-team", OptionMapping::getAsString);
        if (!winnerTeam.equalsIgnoreCase("RED") && !winnerTeam.equalsIgnoreCase("BLUE")) {
            event.reply("❌ 오류: 승리팀은 RED 또는 BLUE로 정확히 입력해 주세요.").setEphemeral(true).queue();
            return;
        }

        try {
            // 세션 시작
            sessionManager.startSession(initiatorId, event.getGuild().getIdLong(), winnerTeam.toUpperCase());

            // 첫 번째 모달 띄우기
            Modal modal = modalBuilder.buildPlayerStatsModal(initiatorId, 1);
            event.replyModal(modal).queue();

        } catch (IllegalStateException e) {
            event.reply("❌ 오류: 이미 진행 중인 경기 등록 세션이 있습니다.").setEphemeral(true).queue();
        }
    }


    /**
     * 모달 제출 이벤트를 처리하고 다음 모달을 띄우거나 최종 등록을 진행합니다.
     */
    public void handleModalSubmission(ModalInteractionEvent event) {

        if (!event.getModalId().startsWith(MODAL_ID_PREFIX)) return; // 해당 핸들러가 처리할 모달인지 확인

        String initiatorId = event.getUser().getId();

        try {
            // 1. 세션 조회 (없으면 IllegalStateException 발생)
            MatchSession session = sessionManager.getSession(initiatorId);

            // 2. 모달 입력 값 파싱 및 DTO 생성
            PlayerStatsDto dto = parseModalInput(event);

            // 3. SessionManager에게 데이터 전달 및 다음 순서 확인
            int nextIndex = sessionManager.addPlayerStats(initiatorId, dto);

            // 4. 다음 단계 진행 결정
            if (nextIndex < 10) {
                // 다음 선수 모달 띄우기
                event.reply("✅ 선수 " + nextIndex + "/10 데이터 입력 완료. 다음 선수 정보를 입력해주세요.")
                        .setEphemeral(true) // 임시 메시지로 응답
                        .queue();
            } else {
                // 최종 등록 및 완료
                handleFinalRegistration(event, initiatorId);
            }

        } catch (NumberFormatException e) {
            event.reply("❌ 오류: KDA 또는 Discord ID는 숫자만 입력해야 합니다. 세션을 종료합니다.").setEphemeral(true).queue();
            sessionManager.removeSession(initiatorId);
        } catch (IllegalArgumentException e) {
            event.reply("❌ 오류: " + e.getMessage() + " 세션을 종료합니다.").setEphemeral(true).queue();
            sessionManager.removeSession(initiatorId);
        } catch (IllegalStateException e) {
            event.reply("❌ 오류: " + e.getMessage() + "\n 다시 `/match-register` 명령어를 사용해주세요.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("경기 기록 처리 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            event.reply("❌ 서버 오류: 경기 기록 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
            sessionManager.removeSession(initiatorId);
        }
    }


    private PlayerStatsDto parseModalInput(ModalInteractionEvent event) throws IllegalArgumentException {
        PlayerStatsDto dto = new PlayerStatsDto();

        // KDA 파싱
        dto.setKills(Integer.parseInt(event.getValue(INPUT_ID_KILLS).getAsString()));
        dto.setDeaths(Integer.parseInt(event.getValue(INPUT_ID_DEATHS).getAsString()));
        dto.setAssists(Integer.parseInt(event.getValue(INPUT_ID_ASSISTS).getAsString()));

        // Discord User ID 파싱 및 유효성 검사 (숫자만 허용)
        dto.setDiscordUserId(Long.parseLong(event.getValue(INPUT_ID_DISCORD_USER).getAsString()));

        // 롤 닉네임 형식 검사
        String fullNickname = event.getValue(INPUT_ID_LOL_NICKNAME).getAsString();
        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("롤 닉네임 형식이 '게임이름#태그'가 아닙니다.");
        }
        dto.setLolGameName(matcher.group(1));
        dto.setLolTagLine(matcher.group(2));

        return dto;
    }

    private void handleFinalRegistration(ModalInteractionEvent event, String initiatorId) {

        try {
            // SessionManager가 최종 DTO를 조립하고 세션을 제거
            MatchRegistrationDto finalDto = sessionManager.assembleAndFinishSession(initiatorId);

            // 서비스 호출 및 DB 저장
            matchRecordService.registerMatch(finalDto);

            // 성공 응답 (ephemeral이 아닌 공개 메시지로)
            event.reply("✅ 경기 기록 등록 완료! **[" + finalDto.getWinnerTeam() + " 팀 승리]** 기록이 데이터베이스에 저장되었습니다.").queue();

        } catch (IllegalArgumentException e) {
            // 서비스에서 발생시킨 유효성 검사 오류 처리 (예: 등록되지 않은 유저/롤 계정)
            event.reply("❌ 등록 오류: " + e.getMessage() + "\n 세션이 종료되었습니다.").queue();
            sessionManager.removeSession(initiatorId); // 안전을 위해 다시 제거
        } catch (Exception e) {
            log.error("경기 기록 최종 등록 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            event.reply("❌ 서버 오류: 경기 기록 등록 중 예기치 않은 오류가 발생했습니다.").queue();
            sessionManager.removeSession(initiatorId);
        }
    }
}