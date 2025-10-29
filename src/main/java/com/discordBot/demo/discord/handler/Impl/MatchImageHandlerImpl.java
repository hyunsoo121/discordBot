package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.discord.presenter.MatchImagePresenter;
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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchImageHandlerImpl implements MatchImageHandler {

    private final ImageAnalysisService imageAnalysisService;
    private final MatchRecordService matchRecordService;
    private final LolAccountRepository lolAccountRepository;
    private final MatchImagePresenter matchImagePresenter;

    private final Map<String, MatchRegistrationDto> pendingConfirmations = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void handleMatchUploadCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue();

        OptionMapping imageOption = event.getOption("result-image");

        if (imageOption == null) {
            event.getHook().sendMessage("❌ 오류: 이미지 파일을 첨부해야 합니다.").queue();
            return;
        }

        Attachment imageAttachment = imageOption.getAsAttachment();
        String initiatorId = event.getUser().getId();
        Long serverId = event.getGuild().getIdLong();

        if (!imageAttachment.isImage()) {
            event.getHook().sendMessage("❌ 오류: 첨부된 파일이 이미지가 아닙니다.").queue();
            return;
        }

        event.getHook().sendMessage(matchImagePresenter.createInitialAnalysisMessage()).queue();

        List<LolAccount> allRegisteredAccounts = lolAccountRepository.findAllByGuildServer_DiscordServerId(serverId);
        log.info("OCR 힌트를 위해 서버 {}에 등록된 계정 {}개를 로드했습니다.", serverId, allRegisteredAccounts.size());

        executor.execute(() -> {
            try {
                MatchRegistrationDto resultDto = imageAnalysisService.analyzeAndStructureData(
                        imageAttachment.getUrl(),
                        serverId,
                        allRegisteredAccounts
                );

                sendConfirmationMessage(event.getHook(), resultDto, initiatorId);

            } catch (IllegalArgumentException e) {
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

    private void sendConfirmationMessage(InteractionHook hook, MatchRegistrationDto dto, String initiatorId) {

        pendingConfirmations.put(initiatorId, dto);

        String messageContent = matchImagePresenter.createConfirmationMessageContent(dto);

        ActionRow buttonRow = matchImagePresenter.createConfirmationButtons(initiatorId);

        hook.editOriginal(messageContent)
                .setComponents(buttonRow) // ActionRow는 하나만 반환되므로 setComponents에 바로 사용
                .queue();
    }

    @Override
    public void handleFinalConfirmation(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");
        String buttonAction = parts[0];
        String requiredInitiatorId = parts[1];
        String actualInitiatorId = event.getUser().getId();

        if (!requiredInitiatorId.equals(actualInitiatorId)) {
            event.getHook().sendMessage("❌ 권한 오류: 원본 업로더만 이 기록을 확정할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        MatchRegistrationDto finalDto = pendingConfirmations.remove(requiredInitiatorId);

        if (finalDto == null) {
            event.getHook().editOriginal("❌ 오류: 이 경기 세션이 만료되었거나 이미 처리되었습니다.").setComponents().queue();
            return;
        }

        if (buttonAction.equals(MatchImageHandler.BUTTON_ID_CONFIRM)) {

            event.getHook().editOriginal("💾 DB에 기록을 저장 중입니다...").setComponents().queue();

            executor.execute(() -> {
                try {
                    matchRecordService.registerMatch(finalDto);

                    event.getHook().editOriginal("✅ **최종 등록 완료!** 경기 기록이 성공적으로 저장되었습니다.")
                            .setComponents()
                            .queue();

                } catch (IllegalArgumentException e) {
                    event.getHook().editOriginal("❌ 등록 오류: " + e.getMessage() + "\n 기록을 다시 확인해주세요.").setComponents().queue();
                    pendingConfirmations.put(requiredInitiatorId, finalDto);
                } catch (Exception e) {
                    log.error("DB 등록 실패: {}", e.getMessage(), e);
                    event.getHook().editOriginal("❌ 서버 오류: 이미지 분석 중 예상치 못한 오류가 발생했습니다.").setComponents().queue();
                }
            });

        } else if (buttonAction.equals(MatchImageHandler.BUTTON_ID_CANCEL)) {
            event.getHook().editOriginal("🚫 경기 기록 등록이 취소되었습니다. `/match-upload`를 다시 사용해 주세요.").setComponents().queue();
        }
    }
}