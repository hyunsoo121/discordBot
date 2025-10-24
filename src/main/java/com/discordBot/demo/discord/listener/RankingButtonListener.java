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
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingButtonListener extends ListenerAdapter {

    private final RankingService rankingService;
    private final RankingHandler rankingHandler;

    private static final String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    private static final String PAGINATION_BUTTON_ID_PREFIX = "page_rank_";
    private static final int MIN_GAMES_THRESHOLD = 1;


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(SORT_BUTTON_ID_PREFIX)) {
            // 정렬 버튼 이벤트
            event.deferEdit().queue();
            Long discordServerId = extractServerId(componentId);
            if (discordServerId != null) {
                handleSortButtonClick(event, discordServerId);
            }
        } else if (componentId.startsWith(PAGINATION_BUTTON_ID_PREFIX)) {
            // ⭐ 페이지네이션 버튼 이벤트
            event.deferEdit().queue();
            Long discordServerId = extractServerId(componentId);
            if (discordServerId != null) {
                handlePageButtonClick(event, discordServerId);
            }
        }
    }

    // --------------------------------------------------------------------------------
    // Helper: 서버 ID 추출
    // --------------------------------------------------------------------------------
    private Long extractServerId(String componentId) {
        try {
            String[] parts = componentId.split("_");
            if (componentId.startsWith(PAGINATION_BUTTON_ID_PREFIX)) {
                // page_rank_CRITERION_SERVERID_PAGEACTION
                return Long.parseLong(parts[parts.length - 2]);
            } else if (componentId.startsWith(SORT_BUTTON_ID_PREFIX)) {
                // sort_rank_CRITERION_SERVERID
                return Long.parseLong(parts[parts.length - 1]);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }


    // --------------------------------------------------------------------------------
    // 정렬 기준 변경 버튼 클릭 이벤트 처리
    // --------------------------------------------------------------------------------
    private void handleSortButtonClick(ButtonInteractionEvent event, Long discordServerId) {
        try {
            // 버튼 ID에서 Enum 이름 추출
            String criterionName = event.getComponentId().substring(
                    SORT_BUTTON_ID_PREFIX.length(),
                    event.getComponentId().lastIndexOf('_')
            );

            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);
            int currentPage = 1; // 정렬 기준 변경 시 1페이지로 리셋

            // 1. 새로운 기준으로 전체 랭킹 조회
            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, newCriterion);
            if (allRankedList.isEmpty()) {
                // 데이터 없음 처리는 handlerCommand에서만 수행해도 무방함
                return;
            }

            // 2. 페이지네이션 정보 계산 및 목록 자르기
            List<UserRankDto> currentPageList = RankingHandler.getPage(allRankedList, currentPage, RankingHandler.ITEMS_PER_PAGE);
            int totalPages = RankingHandler.getTotalPages(allRankedList.size(), RankingHandler.ITEMS_PER_PAGE);

            // 3. 메시지 생성
            MessageEmbed newEmbed = rankingHandler.createDetailedRankingEmbed(discordServerId, event.getGuild().getName(), allRankedList, currentPageList, newCriterion, currentPage, totalPages);

            // 4. 버튼 ActionRow 재생성
            ActionRow sortRow1 = rankingHandler.createSortButtonsRow1(discordServerId, newCriterion);
            ActionRow sortRow2 = rankingHandler.createSortButtonsRow2(discordServerId, newCriterion);
            ActionRow paginationRow = rankingHandler.createPaginationButtonsRowPublic(discordServerId, newCriterion, currentPage, totalPages);


            // 5. 기존 메시지 수정
            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(sortRow1, sortRow2, paginationRow)
                    .queue();

        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ 알 수 없는 정렬 기준입니다.").setEphemeral(true).queue();
        }
    }

    // --------------------------------------------------------------------------------
    // ⭐ 페이지네이션 버튼 클릭 이벤트 처리
    // --------------------------------------------------------------------------------
    private void handlePageButtonClick(ButtonInteractionEvent event, Long discordServerId) {
        try {
            // 버튼 ID 파싱
            String componentId = event.getComponentId();
            String[] parts = componentId.split("_");
            String criterionName = parts[2];
            String pageAction = parts[parts.length - 1];

            RankingCriterion currentCriterion = RankingCriterion.valueOf(criterionName);

            // 1. 현재 Embed에서 현재 페이지/총 페이지 정보를 추출
            MessageEmbed currentEmbed = event.getMessage().getEmbeds().get(0);
            String description = currentEmbed.getDescription();
            int currentPage = 1;
            int totalPages = 1;

            if (description != null) {
                try {
                    // "🔎 **총 N명**의 랭커 중 **P/T페이지** 표시 중" 포맷에서 P/T를 추출
                    int start = description.lastIndexOf("🔎 **총") + 1;
                    String pageInfo = description.substring(start);
                    pageInfo = pageInfo.substring(pageInfo.indexOf("**") + 2, pageInfo.indexOf("페이지"));

                    String[] pageParts = pageInfo.split("/");
                    // P/T 추출 (P와 T를 감싸는 ** 제거)
                    currentPage = Integer.parseInt(pageParts[0].substring(pageParts[0].lastIndexOf('*') + 1).trim());
                    totalPages = Integer.parseInt(pageParts[1].substring(0, pageParts[1].indexOf('*')).trim());

                } catch (Exception e) {
                    // 추출 실패 시 랭킹을 다시 조회하여 totalPages를 계산
                    List<UserRankDto> allListFallback = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);
                    totalPages = RankingHandler.getTotalPages(allListFallback.size(), RankingHandler.ITEMS_PER_PAGE);
                }
            }


            // 2. 새 페이지 번호 계산
            int newPage = currentPage;
            if ("next".equals(pageAction) && currentPage < totalPages) {
                newPage++;
            } else if ("prev".equals(pageAction) && currentPage > 1) {
                newPage--;
            } else {
                // 이미 끝/시작 페이지인 경우: 메시지를 수정할 필요 없이 반환
                return; // 👈 오류 수정: editOriginal().queue() 대신 return;
            }

            // 3. 새로운 기준으로 전체 랭킹 조회
            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);

            // 4. 페이지네이션 정보 계산 및 목록 자르기
            List<UserRankDto> currentPageList = RankingHandler.getPage(allRankedList, newPage, RankingHandler.ITEMS_PER_PAGE);

            // 5. 메시지 생성
            MessageEmbed newEmbed = rankingHandler.createDetailedRankingEmbed(discordServerId, event.getGuild().getName(), allRankedList, currentPageList, currentCriterion, newPage, totalPages);

            // 6. 버튼 ActionRow 재생성
            ActionRow sortRow1 = rankingHandler.createSortButtonsRow1(discordServerId, currentCriterion);
            ActionRow sortRow2 = rankingHandler.createSortButtonsRow2(discordServerId, currentCriterion);
            ActionRow paginationRow = rankingHandler.createPaginationButtonsRowPublic(discordServerId, currentCriterion, newPage, totalPages);

            // 7. 기존 메시지 수정
            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(sortRow1, sortRow2, paginationRow)
                    .queue();


        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ 알 수 없는 정렬 기준입니다.").setEphemeral(true).queue();
        }
    }
}