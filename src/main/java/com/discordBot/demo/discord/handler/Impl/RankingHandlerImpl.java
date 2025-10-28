package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingHandlerImpl implements RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    public static final int ITEMS_PER_PAGE = 10;

    private static final List<RankingCriterion> PRIMARY_CRITERIA = Arrays.asList(
            RankingCriterion.WIN_RATE, RankingCriterion.KDA, RankingCriterion.GAMES
    );
    private static final List<RankingCriterion> SECONDARY_CRITERIA = Arrays.asList(
            RankingCriterion.GPM, RankingCriterion.DPM, RankingCriterion.KP
    );

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

        MessageEmbed detailedEmbed = createDetailedRankingEmbed(serverName, allRankedList, currentPageList, currentCriterion, currentPage, totalPages);
        ActionRow sortRow1 = createSortButtonsRow(discordServerId, currentCriterion, PRIMARY_CRITERIA);
        ActionRow sortRow2 = createSortButtonsRow(discordServerId, currentCriterion, SECONDARY_CRITERIA);
        ActionRow paginationRow = createPaginationButtonsRow(discordServerId, currentCriterion, currentPage, totalPages);

        event.getHook().sendMessageEmbeds(detailedEmbed)
                .setComponents(sortRow1, sortRow2, paginationRow)
                .queue();
    }

    @Override
    public void handleRankingButtonInteraction(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();

        // 정렬 버튼 이벤트 처리
        if (componentId.startsWith(RankingHandler.SORT_BUTTON_ID_PREFIX)) {
            handleSortButtonInternal(event);

            // 페이지네이션 버튼 이벤트 처리
        } else if (componentId.startsWith(RankingHandler.PAGINATION_BUTTON_ID_PREFIX)) {
            handlePaginationButtonInternal(event);
        }
    }

    private void handleSortButtonInternal(ButtonInteractionEvent event) {
        try {
            // 파싱: sort_rank_CRITERION_SERVERID
            String[] parts = event.getComponentId().split("_");
            String criterionName = parts[2];
            Long discordServerId = Long.parseLong(parts[3]);

            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);
            int currentPage = 1;

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, newCriterion);
            if (allRankedList.isEmpty()) return;

            List<UserRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

            MessageEmbed newEmbed = createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, newCriterion, currentPage, totalPages);
            ActionRow sortRow1 = createSortButtonsRow(discordServerId, newCriterion, PRIMARY_CRITERIA);
            ActionRow sortRow2 = createSortButtonsRow(discordServerId, newCriterion, SECONDARY_CRITERIA);
            ActionRow paginationRow = createPaginationButtonsRow(discordServerId, newCriterion, currentPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(sortRow1, sortRow2, paginationRow)
                    .queue();

        } catch (Exception e) {
            log.error("정렬 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 정렬 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private void handlePaginationButtonInternal(ButtonInteractionEvent event) {
        try {
            // 파싱: page_rank_CRITERION_SERVERID_CURRENTPAGE_PAGEACTION
            String componentId = event.getComponentId();
            String[] parts = componentId.split("_");

            String criterionName = parts[2];
            Long discordServerId = Long.parseLong(parts[3]);
            int currentPage = Integer.parseInt(parts[4]);
            String pageAction = parts[5];

            RankingCriterion currentCriterion = RankingCriterion.valueOf(criterionName);

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);
            if (allRankedList.isEmpty()) return;

            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

            // 새 페이지 번호 계산
            int newPage = currentPage;
            if ("next".equals(pageAction) && currentPage < totalPages) {
                newPage++;
            } else if ("prev".equals(pageAction) && currentPage > 1) {
                newPage--;
            } else {
                return;
            }

            // 메시지 및 버튼 재생성
            List<UserRankDto> currentPageList = getPage(allRankedList, newPage, ITEMS_PER_PAGE);
            MessageEmbed newEmbed = createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, currentCriterion, newPage, totalPages);
            ActionRow sortRow1 = createSortButtonsRow(discordServerId, currentCriterion, PRIMARY_CRITERIA);
            ActionRow sortRow2 = createSortButtonsRow(discordServerId, currentCriterion, SECONDARY_CRITERIA);
            ActionRow paginationRow = createPaginationButtonsRow(discordServerId, currentCriterion, newPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(sortRow1, sortRow2, paginationRow)
                    .queue();

        } catch (Exception e) {
            log.error("페이지네이션 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 페이지 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private ActionRow createSortButtonsRow(Long serverId, RankingCriterion activeCriterion, List<RankingCriterion> criteria) {
        return ActionRow.of(criteria.stream()
                .map(criterion -> {
                    String buttonId = RankingHandler.SORT_BUTTON_ID_PREFIX + criterion.name() + "_" + serverId;
                    boolean isActive = criterion == activeCriterion;

                    return isActive
                            ? Button.success(buttonId, "🏆 " + criterion.getDisplayName())
                            : Button.secondary(buttonId, criterion.getDisplayName());
                })
                .collect(Collectors.toList()));
    }

    private ActionRow createPaginationButtonsRow(Long serverId, RankingCriterion activeCriterion, int currentPage, int totalPages) {
        String criterionName = activeCriterion.name();

        Button prevButton = Button.primary(
                        RankingHandler.PAGINATION_BUTTON_ID_PREFIX + criterionName + "_" + serverId + "_" + currentPage + "_prev",
                        "◀️ 이전 페이지")
                .withDisabled(currentPage <= 1);

        Button statusButton = Button.secondary("page_status", currentPage + " / " + totalPages)
                .withDisabled(true);

        Button nextButton = Button.primary(
                        RankingHandler.PAGINATION_BUTTON_ID_PREFIX + criterionName + "_" + serverId + "_" + currentPage + "_next",
                        "다음 페이지 ▶️")
                .withDisabled(currentPage >= totalPages);

        return ActionRow.of(prevButton, statusButton, nextButton);
    }

    private MessageEmbed createDetailedRankingEmbed(String serverName, List<UserRankDto> allRankedList, List<UserRankDto> currentPageList, RankingCriterion criterion, int currentPage, int totalPages) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");

        Color embedColor;
        switch (criterion) {
            case WIN_RATE:
                embedColor = new Color(0, 255, 0);
                break;
            case KDA:
                embedColor = new Color(255, 69, 0);
                break;
            case GAMES:
                embedColor = new Color(173, 216, 230);
                break;
            case GPM:
                embedColor = new Color(255, 215, 0);
                break;
            case DPM:
                embedColor = new Color(255, 0, 0);
                break;
            case KP:
                embedColor = new Color(138, 43, 226);
                break;
            default:
                embedColor = new Color(255, 165, 0);
        }
        embedBuilder.setColor(embedColor);

        embedBuilder.setDescription("기준: **" + criterion.getDisplayName() + "** 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상\n"
                + "🔎 **총 " + allRankedList.size() + "명**의 랭커 중 **" + currentPage + "/" + totalPages + "페이지** 표시 중");


        StringBuilder rankingDetailsField = new StringBuilder();

        rankingDetailsField.append("` 순위 | KDA | GPM | DPM | K P | 승률 | 게임 수`\n");
        rankingDetailsField.append("-------------------------------------------------\n");

        int startRank = (currentPage - 1) * ITEMS_PER_PAGE + 1;

        for (int i = 0; i < currentPageList.size(); i++) {
            UserRankDto dto = currentPageList.get(i);

            String rankSymbol = String.valueOf(startRank + i);
            String performanceEmoji = (dto.getKda() >= 5.0 && dto.getWinRate() * 100 >= 60.0) ? "🔥" : "";
            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            String rankFormat = "`%-5s|%-5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%|%4d` %s %s\n";

            rankingDetailsField.append(
                    String.format(
                            rankFormat,
                            rankSymbol,
                            dto.getKda(),
                            dto.getGpm(),
                            dto.getDpm(),
                            dto.getKillParticipation() * 100,
                            dto.getWinRate() * 100,
                            dto.getTotalGames(),
                            performanceEmoji,
                            userMention
                    )
            );
        }

        embedBuilder.addField("📊 전체 순위표 (상세 지표)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
    }

    public static <T> List<T> getPage(List<T> list, int page, int itemsPerPage) {
        int fromIndex = (page - 1) * itemsPerPage;
        if (fromIndex >= list.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + itemsPerPage, list.size());
        return list.subList(fromIndex, toIndex);
    }
    public static int getTotalPages(int totalItems, int itemsPerPage) {
        if (totalItems <= 0) return 0;
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
}