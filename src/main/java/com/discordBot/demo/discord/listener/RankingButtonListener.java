package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingButtonListener extends ListenerAdapter {

    private final RankingService rankingService;
    private final RankingHandler rankingHandler;

    private static final String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    private static final int MIN_GAMES_THRESHOLD = 1;


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        // 정렬 버튼 이벤트만 처리
        if (componentId.startsWith(SORT_BUTTON_ID_PREFIX)) {

            event.deferEdit().queue(); // 메시지 수정을 위한 응답 승인

            Long discordServerId;
            try {
                // 서버 ID 추출
                discordServerId = Long.parseLong(componentId.substring(componentId.lastIndexOf('_') + 1));
            } catch (Exception e) {
                event.getHook().sendMessage("❌ 서버 정보를 찾을 수 없습니다.").setEphemeral(true).queue();
                return;
            }

            handleSortButtonClick(event, discordServerId);
        }
    }

    /**
     * 정렬 기준 변경 버튼 클릭 이벤트 처리
     */
    private void handleSortButtonClick(ButtonInteractionEvent event, Long discordServerId) {
        try {
            // 버튼 ID에서 Enum 이름 추출
            String criterionName = event.getComponentId().substring(
                    SORT_BUTTON_ID_PREFIX.length(),
                    event.getComponentId().lastIndexOf('_')
            );

            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);

            // 1. 새로운 기준으로 랭킹 조회
            List<UserRankDto> rankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, newCriterion);

            // 2. 메시지 생성 (상세 보기 Embed만 사용)
            MessageEmbed newEmbed = rankingHandler.createDetailedRankingEmbed(discordServerId, event.getGuild().getName(), rankedList, newCriterion);

            // ⭐⭐ 수정: RankingHandler의 Helper를 사용하여 두 개의 ActionRow를 다시 생성
            ActionRow sortRow1 = createUpdatedSortButtonsRow1(discordServerId, newCriterion);
            ActionRow sortRow2 = createUpdatedSortButtonsRow2(discordServerId, newCriterion);


            // 4. 기존 메시지를 새 내용과 두 개의 ActionRow로 수정 (버튼 유지)
            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(sortRow1, sortRow2) // 👈 두 행을 모두 전송하여 정렬 버튼을 유지
                    .queue();

        } catch (IllegalArgumentException e) {
            // Enum.valueOf 실패 시
            event.getHook().sendMessage("❌ 알 수 없는 정렬 기준입니다.").setEphemeral(true).queue();
        }
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper 1: 랭킹 기준 1 (승률, KDA, GAMES) 버튼 행 생성
    // --------------------------------------------------------------------------------
    private ActionRow createUpdatedSortButtonsRow1(Long serverId, RankingCriterion activeCriterion) {
        List<Button> buttons = Arrays.asList(RankingCriterion.WIN_RATE, RankingCriterion.KDA, RankingCriterion.GAMES).stream()
                .map(criterion -> {
                    String buttonId = SORT_BUTTON_ID_PREFIX + criterion.name() + "_" + serverId;
                    boolean isActive = criterion == activeCriterion;

                    return isActive
                            ? Button.success(buttonId, "🏆 " + criterion.getDisplayName())
                            : Button.secondary(buttonId, criterion.getDisplayName());
                })
                .collect(Collectors.toList());
        return ActionRow.of(buttons);
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper 2: 랭킹 기준 2 (GPM, DPM, KP) 버튼 행 생성
    // --------------------------------------------------------------------------------
    private ActionRow createUpdatedSortButtonsRow2(Long serverId, RankingCriterion activeCriterion) {
        List<Button> buttons = Arrays.asList(RankingCriterion.GPM, RankingCriterion.DPM, RankingCriterion.KP).stream()
                .map(criterion -> {
                    String buttonId = SORT_BUTTON_ID_PREFIX + criterion.name() + "_" + serverId;
                    boolean isActive = criterion == activeCriterion;

                    return isActive
                            ? Button.success(buttonId, "🏆 " + criterion.getDisplayName())
                            : Button.secondary(buttonId, criterion.getDisplayName());
                })
                .collect(Collectors.toList());
        return ActionRow.of(buttons);
    }
}