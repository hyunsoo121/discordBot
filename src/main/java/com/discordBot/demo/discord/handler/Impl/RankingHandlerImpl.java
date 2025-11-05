package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.discord.presenter.RankingPresenter;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingHandlerImpl implements RankingHandler {

    private final RankingService rankingService;
    private final RankingPresenter rankingPresenter;
    private static final int MIN_GAMES_THRESHOLD = 1;
    private static final int ITEMS_PER_PAGE = 10;

    @Override
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue();

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();
        RankingCriterion currentCriterion = RankingCriterion.KDA;
        int currentPage = 1;

        List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);

        if (allRankedList.isEmpty()) {
            event.getHook().sendMessage("❌ 현재 서버에는 랭킹 데이터가 없습니다.").queue();
            return;
        }

        List<UserRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
        int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

        MessageEmbed detailedEmbed = rankingPresenter.createDetailedRankingEmbed(serverName, allRankedList, currentPageList, currentCriterion, currentPage, totalPages);
        ActionRow sortRow1 = rankingPresenter.createSortButtonsRow1(discordServerId, currentCriterion);
        ActionRow sortRow2 = rankingPresenter.createSortButtonsRow2(discordServerId, currentCriterion);
        ActionRow paginationRow = rankingPresenter.createPaginationButtonsRow(discordServerId, currentCriterion, currentPage, totalPages);


        // 4. 메시지 전송
        event.getHook().sendMessageEmbeds(detailedEmbed)
                .setComponents(sortRow1, sortRow2, paginationRow)
                .queue();
    }

    @Override
    public void handleRankingButtonInteraction(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();

        // 1. 정렬 버튼 이벤트 처리
        if (componentId.startsWith(RankingHandler.SORT_BUTTON_ID_PREFIX)) {
            handleSortButtonInternal(event);

            // 2. 페이지네이션 버튼 이벤트 처리
        } else if (componentId.startsWith(RankingHandler.PAGINATION_BUTTON_ID_PREFIX)) {
            handlePaginationButtonInternal(event);
        }
    }

    private void handleSortButtonInternal(ButtonInteractionEvent event) {
        try {
            String[] parts = event.getComponentId().split("_");
            String criterionName = parts[2];
            Long discordServerId = Long.parseLong(parts[3]);

            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);
            int currentPage = 1;

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, newCriterion);
            if (allRankedList.isEmpty()) return;

            // 내부 유틸리티 사용
            List<UserRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

            // Presenter 호출
            MessageEmbed newEmbed = rankingPresenter.createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, newCriterion, currentPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createSortButtonsRow1(discordServerId, newCriterion);
            ActionRow sortRow2 = rankingPresenter.createSortButtonsRow2(discordServerId, newCriterion);
            ActionRow paginationRow = rankingPresenter.createPaginationButtonsRow(discordServerId, newCriterion, currentPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (Exception e) {
            log.error("정렬 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 정렬 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private void handlePaginationButtonInternal(ButtonInteractionEvent event) {
        try {
            String componentId = event.getComponentId();
            String[] parts = componentId.split("_");

            String criterionName = parts[2];
            Long discordServerId = Long.parseLong(parts[3]);
            int currentPage = Integer.parseInt(parts[4]);
            String pageAction = parts[5];

            RankingCriterion currentCriterion = RankingCriterion.valueOf(criterionName);

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);
            if (allRankedList.isEmpty()) return;

            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE); // 내부 유틸리티 사용

            // 새 페이지 번호 계산
            int newPage = currentPage;
            if ("next".equals(pageAction) && currentPage < totalPages) {
                newPage++;
            } else if ("prev".equals(pageAction) && currentPage > 1) {
                newPage--;
            } else {
                return;
            }

            List<UserRankDto> currentPageList = getPage(allRankedList, newPage, ITEMS_PER_PAGE); // 내부 유틸리티 사용
            MessageEmbed newEmbed = rankingPresenter.createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, currentCriterion, newPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createSortButtonsRow1(discordServerId, currentCriterion);
            ActionRow sortRow2 = rankingPresenter.createSortButtonsRow2(discordServerId, currentCriterion);
            ActionRow paginationRow = rankingPresenter.createPaginationButtonsRow(discordServerId, currentCriterion, newPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (Exception e) {
            log.error("페이지네이션 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 페이지 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private static <T> List<T> getPage(List<T> list, int page, int itemsPerPage) {
        int fromIndex = (page - 1) * itemsPerPage;
        if (fromIndex >= list.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + itemsPerPage, list.size());
        return list.subList(fromIndex, toIndex);
    }
    private static int getTotalPages(int totalItems, int itemsPerPage) {
        if (totalItems <= 0) return 0;
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
}