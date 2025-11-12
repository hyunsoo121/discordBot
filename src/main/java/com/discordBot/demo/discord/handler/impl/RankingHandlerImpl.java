package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.discord.presenter.RankingPresenter;
import com.discordBot.demo.domain.dto.LineRankDto;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.entity.Line;
import com.discordBot.demo.domain.repository.LineRepository;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingHandlerImpl implements RankingHandler {

    private final RankingService rankingService;
    private final RankingPresenter rankingPresenter;
    private final LineRepository lineRepository;
    private static final int MIN_GAMES_THRESHOLD = 1;
    private static final int ITEMS_PER_PAGE = 10;

    private static final Map<String, String> LINE_ALIASES;

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9가-힣]");

    static {
        LINE_ALIASES = new HashMap<>();

        LINE_ALIASES.put("TOP", "TOP");
        LINE_ALIASES.put("탑", "TOP");
        LINE_ALIASES.put("JUNGLE", "JUNGLE");
        LINE_ALIASES.put("JG", "JUNGLE");
        LINE_ALIASES.put("JGL", "JUNGLE");
        LINE_ALIASES.put("정글", "JUNGLE");
        LINE_ALIASES.put("MID", "MID");
        LINE_ALIASES.put("미드", "MID");
        LINE_ALIASES.put("ADC", "ADC");
        LINE_ALIASES.put("AD", "ADC");
        LINE_ALIASES.put("BOT", "ADC");
        LINE_ALIASES.put("BOTTOM", "ADC");
        LINE_ALIASES.put("원딜", "ADC");
        LINE_ALIASES.put("봇", "ADC");
        LINE_ALIASES.put("SUPPORT", "SUPPORT");
        LINE_ALIASES.put("SUP", "SUPPORT");
        LINE_ALIASES.put("서포터", "SUPPORT");
        LINE_ALIASES.put("서폿", "SUPPORT");
    }
    @Override
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue();

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();
        RankingCriterion currentCriterion = RankingCriterion.WINRATE;
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


        event.getHook().sendMessageEmbeds(detailedEmbed)
                .setComponents(sortRow1, sortRow2, paginationRow)
                .queue();
    }

    @Override
    public void handleLineRankingCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        String rawLineOption = event.getOption("라인이름") != null ? event.getOption("라인이름").getAsString() : null;

        if (rawLineOption == null) {
            event.getHook().sendMessage("❌ 오류: 라인 이름을 입력하세요. (예: TOP, JUNGLE, MID, ADC, SUPPORT)").queue();
            return;
        }

        String cleanedInput = SPECIAL_CHARS_PATTERN.matcher(rawLineOption)
                .replaceAll("")
                .toUpperCase();

        String lineNameForDb = LINE_ALIASES.get(cleanedInput);

        if (lineNameForDb == null) {
            event.getHook().sendMessage("❌ 오류: 알 수 없는 라인 이름입니다: " + rawLineOption).queue();
            return;
        }

        Line line = lineRepository.findByName(lineNameForDb).orElse(null);

        List<LineRankDto> rankedList = rankingService.getLineRanking(discordServerId, line.getLineId(), MIN_GAMES_THRESHOLD, RankingCriterion.WINRATE);

        if (rankedList.isEmpty()) {
            event.getHook().sendMessage("❌ 해당 라인에 대한 랭킹 데이터가 없습니다.").queue();
            return;
        }

        List<LineRankDto> topList = rankedList.subList(0, Math.min(rankedList.size(), ITEMS_PER_PAGE));
        int totalPages = getTotalPages(rankedList.size(), ITEMS_PER_PAGE);

        MessageEmbed embed = rankingPresenter.createLineRankingEmbed(serverName, line.getDisplayName(), rankedList, topList, 1, totalPages);

        ActionRow sortRow1 = rankingPresenter.createLineSortButtonsRow1(discordServerId, line.getLineId(), RankingCriterion.WINRATE);
        ActionRow sortRow2 = rankingPresenter.createLineSortButtonsRow2(discordServerId, line.getLineId(), RankingCriterion.WINRATE);
        ActionRow paginationRow = rankingPresenter.createLinePaginationButtonsRow(discordServerId, line.getLineId(), RankingCriterion.WINRATE, 1, totalPages);

        event.getHook().sendMessageEmbeds(embed)
                .setComponents(sortRow1, sortRow2, paginationRow)
                .queue();
    }

    @Override
    public void handleRankingButtonInteraction(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();

        if (componentId.startsWith(RankingHandler.SORT_BUTTON_ID_PREFIX)) {
            handleSortButtonInternal(event);

        } else if (componentId.startsWith(RankingHandler.PAGINATION_BUTTON_ID_PREFIX)) {
            handlePaginationButtonInternal(event);

        } else if (componentId.startsWith(RankingHandler.SORT_LINE_BUTTON_ID_PREFIX)) {
            handleLineSortButtonInternal(event);

        } else if (componentId.startsWith(RankingHandler.PAGINATION_LINE_BUTTON_ID_PREFIX)) {
            handleLinePaginationButtonInternal(event);
        }
    }

    private void handleLineSortButtonInternal(ButtonInteractionEvent event) {
        try {
            String fullId = event.getComponentId();
            // format: sort_line_rank_CRITERION_SERVERID_LINEID
            String[] parts = fullId.split("_");
            // parts[0]=sort,1=line,2=rank,3=CRITERION,4=SERVERID,5=LINEID (depending on split)
            // Find CRITERION index
            int critIndex = 3;
            String criterionName = parts[critIndex];
            Long discordServerId = Long.parseLong(parts[critIndex + 1]);
            Long lineId = Long.parseLong(parts[critIndex + 2]);

            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);
            int currentPage = 1;

            List<LineRankDto> allRankedList = rankingService.getLineRanking(discordServerId, lineId, MIN_GAMES_THRESHOLD, newCriterion);
            if (allRankedList.isEmpty()) return;

            List<LineRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

            MessageEmbed newEmbed = rankingPresenter.createLineRankingEmbed(event.getGuild().getName(), "라인", allRankedList, currentPageList, currentPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createLineSortButtonsRow1(discordServerId, lineId, newCriterion);
            ActionRow sortRow2 = rankingPresenter.createLineSortButtonsRow2(discordServerId, lineId, newCriterion);
            ActionRow paginationRow = rankingPresenter.createLinePaginationButtonsRow(discordServerId, lineId, newCriterion, currentPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (IllegalArgumentException e) {
            log.error("라인 정렬 버튼 처리 중 Enum 오류 발생 (기준명: {}): {}", event.getComponentId(), e.getMessage());
            event.getHook().sendMessage("❌ 오류: 버튼 ID 파싱 중 알 수 없는 정렬 기준이 발생했습니다.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("라인 정렬 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 라인 정렬 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private void handleLinePaginationButtonInternal(ButtonInteractionEvent event) {
        try {
            String componentId = event.getComponentId();
            String[] parts = componentId.split("_");
            int length = parts.length;

            // ID 구조: page_line_rank_CRITERION_SERVERID_LINEID_CURRENTPAGE_ACTION
            Long discordServerId = Long.parseLong(parts[length - 4]);
            Long lineId = Long.parseLong(parts[length - 3]);
            int currentPage = Integer.parseInt(parts[length - 2]);
            String pageAction = parts[length - 1];

            StringBuilder criterionBuilder = new StringBuilder();
            for (int i = 3; i < length - 4; i++) {
                if (i > 3) criterionBuilder.append("_");
                criterionBuilder.append(parts[i]);
            }
            String criterionName = criterionBuilder.toString();

            RankingCriterion currentCriterion = RankingCriterion.valueOf(criterionName);

            List<LineRankDto> allRankedList = rankingService.getLineRanking(discordServerId, lineId, MIN_GAMES_THRESHOLD, currentCriterion);
            if (allRankedList.isEmpty()) return;

            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);
            int newPage = currentPage;

            if ("next".equals(pageAction) && currentPage < totalPages) {
                newPage++;
            } else if ("prev".equals(pageAction) && currentPage > 1) {
                newPage--;
            } else {
                return;
            }

            List<LineRankDto> currentPageList = getPage(allRankedList, newPage, ITEMS_PER_PAGE);
            MessageEmbed newEmbed = rankingPresenter.createLineRankingEmbed(event.getGuild().getName(), "라인", allRankedList, currentPageList, newPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createLineSortButtonsRow1(discordServerId, lineId, currentCriterion);
            ActionRow sortRow2 = rankingPresenter.createLineSortButtonsRow2(discordServerId, lineId, currentCriterion);
            ActionRow paginationRow = rankingPresenter.createLinePaginationButtonsRow(discordServerId, lineId, currentCriterion, newPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (NumberFormatException e) {
            log.error("라인 페이지네이션 버튼 처리 중 NumberFormatException 발생 (ID 인덱스 오류): {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 오류: 버튼 ID 파싱 중 치명적인 오류가 발생했습니다. 개발자에게 문의하세요.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("라인 페이지네이션 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 페이지 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private void handleSortButtonInternal(ButtonInteractionEvent event) {
        try {
            String fullId = event.getComponentId();
            String[] parts = fullId.split("_");
            int length = parts.length;

            Long discordServerId = Long.parseLong(parts[length - 1]);

            String criterionName = parts[2];
            RankingCriterion newCriterion = RankingCriterion.valueOf(criterionName);
            int currentPage = 1;

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, newCriterion);
            if (allRankedList.isEmpty()) return;

            List<UserRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);

            MessageEmbed newEmbed = rankingPresenter.createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, newCriterion, currentPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createSortButtonsRow1(discordServerId, newCriterion);
            ActionRow sortRow2 = rankingPresenter.createSortButtonsRow2(discordServerId, newCriterion);
            ActionRow paginationRow = rankingPresenter.createPaginationButtonsRow(discordServerId, newCriterion, currentPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (IllegalArgumentException e) {
            log.error("정렬 버튼 처리 중 Enum 오류 발생 (기준명: {}): {}", event.getComponentId(), e.getMessage());
            event.getHook().sendMessage("❌ 오류: 버튼 ID 파싱 중 알 수 없는 정렬 기준이 발생했습니다.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("정렬 버튼 처리 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 정렬 처리 중 오류가 발생했습니다.").setEphemeral(true).queue();
        }
    }

    private void handlePaginationButtonInternal(ButtonInteractionEvent event) {
        try {
            String componentId = event.getComponentId();
            String[] parts = componentId.split("_");
            int length = parts.length;

            // ID 구조: page_rank_CRITERION_SERVERID_CURRENTPAGE_ACTION (총 7 파트)

            Long discordServerId = Long.parseLong(parts[length - 3]);

            int currentPage = Integer.parseInt(parts[length - 2]);

            String pageAction = parts[length - 1]; // ACTION (next/prev)

            StringBuilder criterionBuilder = new StringBuilder();
            for (int i = 2; i < length - 3; i++) {
                if (i > 2) {
                    criterionBuilder.append("_");
                }
                criterionBuilder.append(parts[i]);
            }
            String criterionName = criterionBuilder.toString();

            RankingCriterion currentCriterion = RankingCriterion.valueOf(criterionName);

            List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);
            if (allRankedList.isEmpty()) return;

            int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);
            int newPage = currentPage;

            if ("next".equals(pageAction) && currentPage < totalPages) {
                newPage++;
            } else if ("prev".equals(pageAction) && currentPage > 1) {
                newPage--;
            } else {
                return;
            }

            List<UserRankDto> currentPageList = getPage(allRankedList, newPage, ITEMS_PER_PAGE);
            MessageEmbed newEmbed = rankingPresenter.createDetailedRankingEmbed(event.getGuild().getName(), allRankedList, currentPageList, currentCriterion, newPage, totalPages);
            ActionRow sortRow1 = rankingPresenter.createSortButtonsRow1(discordServerId, currentCriterion);
            ActionRow sortRow2 = rankingPresenter.createSortButtonsRow2(discordServerId, currentCriterion);
            ActionRow paginationRow = rankingPresenter.createPaginationButtonsRow(discordServerId, currentCriterion, newPage, totalPages);

            event.getHook().editOriginalEmbeds(newEmbed).setComponents(sortRow1, sortRow2, paginationRow).queue();

        } catch (NumberFormatException e) {
            log.error("페이지네이션 버튼 처리 중 NumberFormatException 발생 (ID 인덱스 오류): {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 오류: 버튼 ID 파싱 중 치명적인 오류가 발생했습니다. 개발자에게 문의하세요.").setEphemeral(true).queue();
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