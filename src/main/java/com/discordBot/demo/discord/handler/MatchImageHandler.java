package com.discordBot.demo.discord.handler;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchImageHandler {

    private final ImageAnalysisService imageAnalysisService;
    private final MatchRecordService matchRecordService;

    // Button Constants
    public static final String BUTTON_ID_CONFIRM = "match-confirm";
    public static final String BUTTON_ID_CANCEL = "match-cancel";

    // Temporary storage for data awaiting confirmation
    private final Map<String, MatchRegistrationDto> pendingConfirmations = new ConcurrentHashMap<>();

    // Executor for handling long-running Gemini API calls and DB operations asynchronously
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    /**
     * Handles the /match-upload command, defers reply, and submits image for AI analysis.
     */
    public void handleMatchUploadCommand(SlashCommandInteractionEvent event) {

        // Defer reply immediately (public response) to handle long processing time
        event.deferReply(false).queue();

        OptionMapping winnerTeamOption = event.getOption("winner-team");
        OptionMapping imageOption = event.getOption("result-image");

        if (winnerTeamOption == null || imageOption == null) {
            event.getHook().sendMessage("❌ Error: Both winner team and image file must be provided.").queue();
            return;
        }

        String winnerTeam = winnerTeamOption.getAsString().toUpperCase();
        Attachment imageAttachment = imageOption.getAsAttachment();
        String initiatorId = event.getUser().getId();

        // 1. Basic validation
        if (!winnerTeam.equals("RED") && !winnerTeam.equals("BLUE")) {
            event.getHook().sendMessage("❌ Error: Winner team must be RED or BLUE.").queue();
            return;
        }
        if (!imageAttachment.isImage()) {
            event.getHook().sendMessage("❌ Error: The attached file is not an image.").queue();
            return;
        }

        // 2. Initial status update (Hook must be used after deferReply)
        event.getHook().sendMessage("🔍 Analyzing image. Please wait... (AI processing)").queue();

        // 3. Execute long-running AI process on a separate thread
        executor.execute(() -> {
            try {
                // Call image analysis service (Gemini)
                MatchRegistrationDto resultDto = imageAnalysisService.analyzeAndStructureData(
                        imageAttachment.getUrl(),
                        winnerTeam,
                        event.getGuild().getIdLong()
                );

                // Analysis successful: Send confirmation message
                sendConfirmationMessage(event.getHook(), resultDto, initiatorId);

            } catch (Exception e) {
                log.error("Error during match record processing: {}", e.getMessage(), e);
                // Edit original message to display error
                event.getHook().editOriginal("❌ Server Error: An unexpected error occurred during image analysis. Check logs.")
                        .setComponents()
                        .queue();
            }
        });
    }

    /**
     * Sends the final confirmation message with CONFIRM/CANCEL buttons to the user.
     */
    private void sendConfirmationMessage(InteractionHook hook, MatchRegistrationDto dto, String initiatorId) {

        // 1. Store data temporarily for button handling
        pendingConfirmations.put(initiatorId, dto);

        // 2. Create message body
        StringBuilder sb = new StringBuilder();
        sb.append("✅ **AI Analysis Complete!** Is the following record correct? (Only the uploader can confirm)\n\n");
        sb.append("🏆 Winning Team: **").append(dto.getWinnerTeam()).append("**\n\n");

        // Summarize player stats
        dto.getPlayerStatsList().forEach(stats -> {
            sb.append("`").append(stats.getTeam()).append("` | ");
            sb.append(stats.getLolGameName()).append("#").append(stats.getLolTagLine()).append(" | ");
            sb.append("KDA: ").append(stats.getKills()).append("/").append(stats.getDeaths()).append("/").append(stats.getAssists()).append("\n");
        });

        // 3. Create buttons (with initiator ID for permission check)
        Button confirmButton = Button.success(BUTTON_ID_CONFIRM + ":" + initiatorId, "✅ Final Registration");
        Button cancelButton = Button.danger(BUTTON_ID_CANCEL + ":" + initiatorId, "❌ Cancel / Modify");

        // 4. Edit original message using Hook, adding buttons
        hook.editOriginal(sb.toString())
                .setComponents(ActionRow.of(confirmButton, cancelButton))
                .queue();
    }

    /**
     * Handles the button click event for final confirmation or cancellation.
     */
    public void handleFinalConfirmation(ButtonInteractionEvent event) {

        // 1. Get IDs and button action
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");
        String buttonAction = parts[0];
        String requiredInitiatorId = parts[1];
        String actualInitiatorId = event.getUser().getId();

        // NOTE: event.deferEdit() is called in SlashCommandListener.

        // Permission check
        if (!requiredInitiatorId.equals(actualInitiatorId)) {
            event.getHook().sendMessage("❌ Permission Error: Only the original uploader can finalize this record.").setEphemeral(true).queue();
            return;
        }

        // Retrieve and remove pending data
        MatchRegistrationDto finalDto = pendingConfirmations.remove(requiredInitiatorId);

        if (finalDto == null) {
            event.getHook().editOriginal("❌ Error: This match session has expired or was already processed.").setComponents().queue();
            return;
        }

        // 2. Process button action
        if (buttonAction.equals(BUTTON_ID_CONFIRM)) {

            // ⭐⭐⭐ 핵심 수정: DB 저장 로직을 Executor 내부로 이동 ⭐⭐⭐
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
            // ⭐⭐⭐ 수정 끝 ⭐⭐⭐

        } else if (buttonAction.equals(BUTTON_ID_CANCEL)) {
            // Cancellation
            event.getHook().editOriginal("🚫 Match registration has been cancelled. Please use `/match-upload` again.").setComponents().queue();
        }
    }
}
