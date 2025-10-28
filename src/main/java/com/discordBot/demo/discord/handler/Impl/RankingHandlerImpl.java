package com.discordBot.demo.discord.handler.Impl;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingHandlerImpl implements RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    public static final int ITEMS_PER_PAGE = 10;
    private static final String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    public static final String PAGINATION_BUTTON_ID_PREFIX = "page_rank_";

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

        // 초기 설정: KDA 기준으로 정렬 및 1페이지 시작
        RankingCriterion currentCriterion = RankingCriterion.KDA;
        int currentPage = 1;

        // DB에서 전체 랭킹 데이터를 가져옴
        List<UserRankDto> allRankedList = rankingService.getRanking(discordServerId, MIN_GAMES_THRESHOLD, currentCriterion);

        if (allRankedList.isEmpty()) {
            event.getHook().sendMessage("❌ 현재 서버에는 랭킹 데이터가 없습니다.").queue();
            return;
        }

        // 페이지네이션 정보 계산 및 목록 자르기
        List<UserRankDto> currentPageList = getPage(allRankedList, currentPage, ITEMS_PER_PAGE);
        int totalPages = getTotalPages(allRankedList.size(), ITEMS_PER_PAGE);


        // 1. Embed 생성
        MessageEmbed detailedEmbed = createDetailedRankingEmbed(serverName, allRankedList, currentPageList, currentCriterion, currentPage, totalPages);

        // 2. 정렬 버튼 생성
        ActionRow sortRow1 = createSortButtonsRow(discordServerId, currentCriterion, PRIMARY_CRITERIA);
        ActionRow sortRow2 = createSortButtonsRow(discordServerId, currentCriterion, SECONDARY_CRITERIA);

        // 3. 페이지네이션 버튼 생성
        ActionRow paginationRow = createPaginationButtonsRow(discordServerId, currentCriterion, currentPage, totalPages);


        // 4. 메시지 전송
        event.getHook().sendMessageEmbeds(detailedEmbed)
                .setComponents(
                        sortRow1,
                        sortRow2,
                        paginationRow
                )
                .queue();
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 정렬 버튼 행 생성 (공통 메서드)
    // --------------------------------------------------------------------------------
    private ActionRow createSortButtonsRow(Long serverId, RankingCriterion activeCriterion, List<RankingCriterion> criteria) {
        List<Button> buttons = criteria.stream()
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
    // ⭐ Helper: 페이지네이션 버튼 행 생성
    // --------------------------------------------------------------------------------
    private ActionRow createPaginationButtonsRow(Long serverId, RankingCriterion activeCriterion, int currentPage, int totalPages) {
        String criterionName = activeCriterion.name();

        // ID 포맷: page_rank_CRITERION_SERVERID_PAGEACTION
        Button prevButton = Button.primary(
                        PAGINATION_BUTTON_ID_PREFIX + criterionName + "_" + serverId + "_prev",
                        "◀️ 이전 페이지")
                .withDisabled(currentPage <= 1);

        Button statusButton = Button.secondary("page_status", currentPage + " / " + totalPages)
                .withDisabled(true); // 클릭 불가

        Button nextButton = Button.primary(
                        PAGINATION_BUTTON_ID_PREFIX + criterionName + "_" + serverId + "_next",
                        "다음 페이지 ▶️")
                .withDisabled(currentPage >= totalPages);

        return ActionRow.of(prevButton, statusButton, nextButton);
    }

    public MessageEmbed createDetailedRankingEmbed(String serverName, List<UserRankDto> allRankedList, List<UserRankDto> currentPageList, RankingCriterion criterion, int currentPage, int totalPages) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");

        Color embedColor;
        switch (criterion) {
            case WIN_RATE:
                embedColor = new Color(0, 255, 0); // 승률 (초록색): 승리를 강조
                break;
            case KDA:
                embedColor = new Color(255, 69, 0); // KDA (주황-빨강): 개인 역량을 강조
                break;
            case GAMES:
                embedColor = new Color(173, 216, 230); // 게임 수 (연한 파랑): 활동량을 강조
                break;
            case GPM:
                embedColor = new Color(255, 215, 0); // GPM (금색): 골드 수급력을 강조
                break;
            case DPM:
                embedColor = new Color(255, 0, 0); // DPM (빨간색): 딜링 능력을 강조
                break;
            case KP:
                embedColor = new Color(138, 43, 226); // KP (보라색): 팀 기여도를 강조
                break;
            default:
                embedColor = new Color(255, 165, 0); // 기본 (주황색)
        }
        embedBuilder.setColor(embedColor); // 설정된 색상을 적용

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

            // KDA 왼쪽 정렬 반영됨: %-5.2f
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
    public ActionRow createSortButtonsRow1(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, PRIMARY_CRITERIA);
    }

    public ActionRow createSortButtonsRow2(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, SECONDARY_CRITERIA);
    }

    public ActionRow createPaginationButtonsRowPublic(Long serverId, RankingCriterion activeCriterion, int currentPage, int totalPages) {
        return createPaginationButtonsRow(serverId, activeCriterion, currentPage, totalPages);
    }
}