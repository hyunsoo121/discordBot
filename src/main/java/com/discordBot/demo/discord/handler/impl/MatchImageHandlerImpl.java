package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.discord.presenter.MatchImagePresenter;
import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.ImageAnalysisService;
import com.discordBot.demo.service.ChampionService;
import com.discordBot.demo.service.TemporaryMatchStorageService; // ⭐ Storage Service 재추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchImageHandlerImpl implements MatchImageHandler {

    private final ImageAnalysisService imageAnalysisService;
    private final MatchRecordService matchRecordService;
    private final LolAccountRepository lolAccountRepository;
    private final MatchImagePresenter matchImagePresenter;
    private final ChampionService championService;
    private final TemporaryMatchStorageService storageService; // ⭐ Storage Service 재추가

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // -----------------------------------------------------------
    // 1. 슬래시 커맨드 처리 (분석 및 임시 저장)
    // -----------------------------------------------------------

    @Override
    public void handleMatchUploadCommand(SlashCommandInteractionEvent event) {

        if (!event.isAcknowledged()) {
            event.deferReply(true).queue();
        }

        OptionMapping imageOption = event.getOption("input-image");

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

        event.getHook().editOriginal(matchImagePresenter.createInitialAnalysisMessage()).queue();

        List<LolAccount> allRegisteredAccounts = lolAccountRepository.findAllByGuildServer_DiscordServerId(serverId);
        log.info("OCR 힌트를 위해 서버 {}에 등록된 계정 {}개를 로드했습니다.", serverId, allRegisteredAccounts.size());

        executor.execute(() -> {
            try {
                MatchRegistrationDto resultDto = imageAnalysisService.analyzeAndStructureData(
                        imageAttachment.getUrl(),
                        serverId,
                        allRegisteredAccounts
                );

                Long tempMatchId = storageService.saveTemporaryMatch(resultDto);

                sendConfirmationMessage(event.getHook(), resultDto, initiatorId, tempMatchId);

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

    private void sendConfirmationMessage(InteractionHook hook, MatchRegistrationDto dto, String initiatorId, Long tempMatchId) {

        String messageContent = matchImagePresenter.createConfirmationMessageContent(dto);

        List<ActionRow> buttonRows = matchImagePresenter.createConfirmationButtonsWithId(initiatorId, tempMatchId, dto.getPlayerStatsList());

        hook.editOriginal(messageContent)
                .setComponents(buttonRows)
                .queue();
    }

    // -----------------------------------------------------------
    // 2. 버튼 인터랙션 처리 (확인, 취소, 수정)
    // -----------------------------------------------------------

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");
        String buttonAction = parts[0];
        String requiredInitiatorId = parts[1];

        if (!requiredInitiatorId.equals(event.getUser().getId())) {
            event.reply("❌ 권한 오류: 원본 업로더만 이 기록을 확정/수정할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        Long tempMatchId = Long.parseLong(parts[2]);

        // CONFIRM / CANCEL은 DB 작업이 있으므로 deferReply를 시도해야 합니다.
        if (buttonAction.equals(MatchImageHandler.BUTTON_ID_CONFIRM) || buttonAction.equals(MatchImageHandler.BUTTON_ID_CANCEL)) {
            event.deferReply(true).queue(); // Hook 사용을 위한 deferral

            MatchRegistrationDto finalDto = storageService.getTemporaryMatch(tempMatchId);
            storageService.removeTemporaryMatch(tempMatchId);

            if (finalDto == null) {
                event.getHook().editOriginal("❌ 오류: 이 경기 세션이 만료되었거나 이미 처리되었습니다.").setComponents().queue();
                return;
            }

            if (buttonAction.equals(MatchImageHandler.BUTTON_ID_CONFIRM)) {
                handleConfirm(event, finalDto);
            } else if (buttonAction.equals(MatchImageHandler.BUTTON_ID_CANCEL)) {
                event.getHook().editOriginal("🚫 경기 기록 등록이 취소되었습니다. `/match-upload`를 다시 사용해 주세요.").setComponents().queue();
            }
            return;
        }

        // EDIT 버튼 클릭 처리
        if (buttonAction.equals(MatchImageHandler.BUTTON_ID_EDIT)) {
            // ID 포맷: BUTTON_ID_EDIT:INITIATOR_ID:TEMP_MATCH_ID:TEAM:CATEGORY
            String teamFilter = parts[3];
            String category = parts[4];
            handleEditButton(event, tempMatchId, teamFilter, category);
        }
    }

    // 최종 등록 로직
    private void handleConfirm(ButtonInteractionEvent event, MatchRegistrationDto finalDto) {
        event.getHook().editOriginal("💾 DB에 기록을 저장 중입니다...").setComponents().queue();

        executor.execute(() -> {
            try {
                matchRecordService.registerMatch(finalDto);

                event.getHook().editOriginal("✅ **최종 등록 완료!** 경기 기록이 성공적으로 저장되었습니다.")
                        .setComponents()
                        .queue();

            } catch (IllegalArgumentException e) {
                log.error("DB 등록 오류 (비즈니스): {}", e.getMessage(), e);
                event.getHook().editOriginal("❌ 등록 오류: " + e.getMessage() + "\n 기록을 다시 확인해주세요. 재시도는 `/match-upload`를 사용하세요.").setComponents().queue();
            } catch (Exception e) {
                log.error("DB 등록 실패: {}", e.getMessage(), e);
                event.getHook().editOriginal("❌ 서버 오류: 기록 저장 중 예상치 못한 오류가 발생했습니다. 로그를 확인하세요.").setComponents().queue();
            }
        });
    }

    // 모달 띄우기 로직
    private void handleEditButton(ButtonInteractionEvent event, Long tempMatchId, String teamFilter, String category) {

        MatchRegistrationDto dto = storageService.getTemporaryMatch(tempMatchId);
        if (dto == null) {
            event.reply("❌ 오류: 이 경기 세션이 만료되었거나 이미 처리되었습니다.").setEphemeral(true).queue();
            return;
        }

        List<PlayerStatsDto> playersToEdit = dto.getPlayerStatsList().stream()
                .filter(p -> p.getTeam().equalsIgnoreCase(teamFilter))
                .collect(Collectors.toList());

        // ⭐ 모달 생성 시 tempMatchId와 category를 ID에 포함하여 제출 시 복원 가능하도록 함
        Modal editModal = createEditModal(playersToEdit, tempMatchId, teamFilter, category);

        // ⭐ 모달 생성 및 응답: defer 없이 replyModal로 즉시 응답하여 충돌 방지
        event.replyModal(editModal).queue();
    }

    // 모달 생성 헬퍼
    private Modal createEditModal(List<PlayerStatsDto> players, Long tempMatchId, String teamFilter, String category) {

        String teamLabel = teamFilter.equals("BLUE") ? "🟦 블루팀" : "🟥 레드팀";
        String categoryLabel;
        String componentIdPrefix;

        switch (category) {
            case "CHAMP":
                categoryLabel = "챔피언 이름";
                componentIdPrefix = "C_";
                break;
            case "LANE":
                categoryLabel = "라인 (TOP, JUNGLE, MID, ADC, SUPPORT)";
                componentIdPrefix = "L_";
                break;
            case "ACCOUNT":
                categoryLabel = "Riot 계정 이름 (이름#태그)";
                componentIdPrefix = "A_";
                break;
            default:
                throw new IllegalArgumentException("유효하지 않은 수정 카테고리입니다: " + category);
        }

        // ID 포맷: MODAL_ID_BASE:TEMP_MATCH_ID:TEAM:CATEGORY
        Modal.Builder modalBuilder = Modal.create(
                MatchImageHandler.MODAL_ID_BASE + ":" + tempMatchId + ":" + teamFilter + ":" + category,
                teamLabel + " " + categoryLabel + " 수정"
        );

        // 5명의 플레이어 데이터를 5개의 TextInput으로 생성 (Modal 제한 5개 준수)
        for (int i = 0; i < 5; i++) {
            PlayerStatsDto player = players.get(i);
            String initialValue;

            if (category.equals("CHAMP")) {
                initialValue = player.getChampionName();
            } else if (category.equals("LANE")) {
                initialValue = player.getLaneName();
            } else { // ACCOUNT
                initialValue = player.getLolGameName() + "#" + player.getLolTagLine();
            }

            String label = String.format("%d. %s", i + 1, player.getLolGameName());
            String componentId = componentIdPrefix + i;

            TextInput input = TextInput.create(componentId, label, TextInputStyle.SHORT)
                    .setValue(initialValue.equalsIgnoreCase("UNKNOWN") ? "" : initialValue)
                    .setPlaceholder("현재 값: " + initialValue)
                    .setRequired(true)
                    .build();

            modalBuilder.addActionRow(input);
        }

        return modalBuilder.build();
    }

    // -----------------------------------------------------------
    // 3. 모달 인터랙션 처리
    // -----------------------------------------------------------

    @Override
    public void handleModalInteraction(ModalInteractionEvent event) {

        String modalId = event.getModalId();
        String[] parts = modalId.split(":");
        if (!parts[0].equals(MatchImageHandler.MODAL_ID_BASE)) return; // 모달 ID 확인

        // ID 포맷: MODAL_ID_BASE:TEMP_MATCH_ID:TEAM:CATEGORY
        Long tempMatchId = Long.parseLong(parts[1]);
        String teamFilter = parts[2];
        String category = parts[3];

        MatchRegistrationDto dto = storageService.getTemporaryMatch(tempMatchId);

        if (dto == null) {
            event.reply("❌ 오류: 수정 세션이 만료되었습니다. 다시 시도해 주세요.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        List<PlayerStatsDto> playersToEdit = dto.getPlayerStatsList().stream()
                .filter(p -> p.getTeam().equalsIgnoreCase(teamFilter))
                .collect(Collectors.toList());

        try {
            // ⭐ 모달 데이터 추출 및 DTO 업데이트
            for (int i = 0; i < 5; i++) {
                PlayerStatsDto player = playersToEdit.get(i);
                String componentIdPrefix = category.substring(0, 1) + "_";
                String componentId = componentIdPrefix + i;

                String newValue = event.getValue(componentId).getAsString().trim();

                if (category.equals("CHAMP")) {
                    // 유효성 검사: championService.findChampionByIdentifier(newValue).isEmpty()
                    if (championService.findChampionByIdentifier(newValue).isEmpty()) { throw new IllegalArgumentException("유효하지 않은 챔피언 이름입니다."); }
                    player.setChampionName(newValue);
                } else if (category.equals("LANE")) {
                    // 유효성 검사: isValidLane(newValue.toUpperCase())
                    if (!isValidLane(newValue.toUpperCase())) { throw new IllegalArgumentException("유효하지 않은 라인 정보입니다. (TOP, JUNGLE, MID, ADC, SUPPORT)"); }
                    player.setLaneName(newValue.toUpperCase());
                } else { // ACCOUNT
                    // 유효성 검사: parseLolNameTag, lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId
                    String[] partsLol = parseLolNameTag(newValue);
                    Long serverId = event.getGuild().getIdLong();
                    Optional<LolAccount> accountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                            partsLol[0], partsLol[1], serverId);

                    if (accountOpt.isEmpty()) { throw new IllegalArgumentException("서버에 등록되지 않은 계정입니다."); }

                    player.setLolGameName(partsLol[0]);
                    player.setLolTagLine(partsLol[1]);
                }
            }

            storageService.updateTemporaryMatch(tempMatchId, dto);

            String messageContent = matchImagePresenter.createConfirmationMessageContent(dto);
            List<ActionRow> buttonRows = matchImagePresenter.createConfirmationButtonsWithId(event.getUser().getId(), tempMatchId, dto.getPlayerStatsList());

            event.getHook().editOriginal(messageContent)
                    .setComponents(buttonRows)
                    .queue();

            event.getHook().sendMessage("✅ 기록이 성공적으로 수정되었습니다.").setEphemeral(true).queue();

        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ 수정 오류: " + e.getMessage()).setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("모달 제출 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 서버 오류: 기록 수정 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    // ⭐ 헬퍼 메서드: 라인 체크 (이전 논의에서 재사용)
    private boolean isValidLane(String lane) {
        return List.of("TOP", "JUNGLE", "MID", "ADC", "SUPPORT").contains(lane);
    }

    // ⭐ 헬퍼 메서드: 계정명#태그라인 파싱 (이전 논의에서 재사용)
    private String[] parseLolNameTag(String lolNameTag) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("(.+)#(.+)");
        Matcher matcher = pattern.matcher(lolNameTag);

        if (matcher.matches() && matcher.groupCount() == 2) {
            return new String[]{matcher.group(1), matcher.group(2)};
        } else {
            throw new IllegalArgumentException("라이엇 계정명은 '이름#태그' 형식이어야 합니다.");
        }
    }
}