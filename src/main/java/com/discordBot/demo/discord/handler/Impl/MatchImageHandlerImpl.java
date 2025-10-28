package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.ImageAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; // ⭐ Executors 임포트

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchImageHandlerImpl implements MatchImageHandler {

    private final ImageAnalysisService imageAnalysisService;
    private final MatchRecordService matchRecordService;
    private final LolAccountRepository lolAccountRepository;

    private final Map<String, MatchRegistrationDto> pendingConfirmations = new ConcurrentHashMap<>();

    // 오래 걸리는 AI 작업을 병렬 처리하기 위해 CachedThreadPool 사용
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void handleMatchUploadCommand(SlashCommandInteractionEvent event) {

        // SlashCommandListener에서 event.deferReply(true)를 이미 호출했다고 가정합니다.

        OptionMapping imageOption = event.getOption("result-image");

        if (imageOption == null) {
            event.getHook().sendMessage("❌ 오류: 이미지 파일을 첨부해야 합니다.").queue();
            return;
        }

        Attachment imageAttachment = imageOption.getAsAttachment();
        String initiatorId = event.getUser().getId();
        Long serverId = event.getGuild().getIdLong();

        // 기본 유효성 검사(이미지가 맞는지)
        if (!imageAttachment.isImage()) {
            event.getHook().sendMessage("❌ 오류: 첨부된 파일이 이미지가 아닙니다.").queue();
            return;
        }

        event.getHook().sendMessage("🔍 이미지를 분석 중입니다. 잠시 기다려 주세요... (AI 처리)").queue();

        // 롤 계정 후보 목록 조회 (OCR 힌트 준비)
        List<LolAccount> allRegisteredAccounts = lolAccountRepository.findAllByGuildServer_DiscordServerId(serverId);
        log.info("OCR 힌트를 위해 서버 {}에 등록된 계정 {}개를 로드했습니다.", serverId, allRegisteredAccounts.size());

        // 별도의 스레드에서 오래 걸리는 AI 프로세스 실행
        executor.execute(() -> {
            try {
                MatchRegistrationDto resultDto = imageAnalysisService.analyzeAndStructureData(
                        imageAttachment.getUrl(),
                        serverId,
                        allRegisteredAccounts
                );

                // 분석 성공: 확인 메시지 전송
                sendConfirmationMessage(event.getHook(), resultDto, initiatorId);

            } catch (IllegalArgumentException e) {
                // ImageAnalysisService에서 던진 '승패 텍스트 없음'과 같은 사용자 오류 처리
                event.getHook().editOriginal("❌ 분석 오류: " + e.getMessage())
                        .setComponents()
                        .queue();
            } catch (Exception e) {
                log.error("경기 기록 처리 중 오류 발생: {}", e.getMessage(), e);
                event.getHook().editOriginal("❌ 서버 오류: 이미지 분석 중 예상치 못한 오류가 발생했습니다. 로그를 확인하세요.")
                        .setComponents()
                        .queue();
            }
        });
    }

    private String getDisplayTeamLabel(String dbTeamSide) {
        if (dbTeamSide.equals("BLUE")) {
            return "1팀";
        } else if (dbTeamSide.equals("RED")) {
            return "2팀";
        }
        return dbTeamSide;
    }

    private void sendConfirmationMessage(InteractionHook hook, MatchRegistrationDto dto, String initiatorId) {

        pendingConfirmations.put(initiatorId, dto);

        StringBuilder sb = new StringBuilder();
        sb.append("✅ **AI 분석 완료!** 아래 기록이 정확합니까? (업로더만 확인할 수 있습니다)\n\n");

        String winnerTeamLabel = getDisplayTeamLabel(dto.getWinnerTeam());
        sb.append("🏆 승리팀: **").append(winnerTeamLabel).append("**\n\n");

        dto.getPlayerStatsList().forEach(stats -> {
            String displayTeamLabel = getDisplayTeamLabel(stats.getTeam());

            sb.append("`").append(displayTeamLabel).append("` | ");
            sb.append(stats.getLolGameName()).append("#").append(stats.getLolTagLine()).append(" | ");
            sb.append("KDA: ").append(stats.getKills()).append("/").append(stats.getDeaths()).append("/").append(stats.getAssists()).append("\n");
        });

        Button confirmButton = Button.success(BUTTON_ID_CONFIRM + ":" + initiatorId, "✅ 최종 등록");
        Button cancelButton = Button.danger(BUTTON_ID_CANCEL + ":" + initiatorId, "❌ 취소 / 수정");

        // Hook을 사용하여 원본 메시지 수정 및 버튼 추가
        hook.editOriginal(sb.toString())
                .setComponents(ActionRow.of(confirmButton, cancelButton))
                .queue();
    }

    @Override
    public void handleFinalConfirmation(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");
        String buttonAction = parts[0];
        String requiredInitiatorId = parts[1];
        String actualInitiatorId = event.getUser().getId();


        // 권한 확인
        if (!requiredInitiatorId.equals(actualInitiatorId)) {
            event.getHook().sendMessage("❌ 권한 오류: 원본 업로더만 이 기록을 확정할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        // 대기 중인 데이터 검색 및 제거
        MatchRegistrationDto finalDto = pendingConfirmations.remove(requiredInitiatorId);

        if (finalDto == null) {
            event.getHook().editOriginal("❌ 오류: 이 경기 세션이 만료되었거나 이미 처리되었습니다.").setComponents().queue();
            return;
        }

        if (buttonAction.equals(BUTTON_ID_CONFIRM)) {

            // DB 저장 로직을 Executor 내부로 이동
            event.getHook().editOriginal("💾 DB에 기록을 저장 중입니다...").setComponents().queue(); // 사용자에게 저장 중임을 알림

            executor.execute(() -> {
                try {
                    // DB 저장 로직 실행
                    matchRecordService.registerMatch(finalDto);

                    // 메시지 수정 및 컴포넌트 제거
                    event.getHook().editOriginal("✅ **최종 등록 완료!** 경기 기록이 성공적으로 저장되었습니다.")
                            .setComponents()
                            .queue();

                } catch (IllegalArgumentException e) {
                    event.getHook().editOriginal("❌ 등록 오류: " + e.getMessage() + "\n 기록을 다시 확인해주세요.").setComponents().queue();
                    pendingConfirmations.put(requiredInitiatorId, finalDto); // DB 오류 시 데이터 복구 (취소/재시도 가능성 대비)
                } catch (Exception e) {
                    log.error("DB 등록 실패: {}", e.getMessage(), e);
                    event.getHook().editOriginal("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").setComponents().queue();
                }
            });

        } else if (buttonAction.equals(BUTTON_ID_CANCEL)) {
            // 취소
            event.getHook().editOriginal("🚫 경기 기록 등록이 취소되었습니다. `/match-upload`를 다시 사용해 주세요.").setComponents().queue();
        }
    }
}