package com.discordBot.demo.discord.handler;

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
public class RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    // ⭐ 페이지네이션 관련 상수
    public static final int ITEMS_PER_PAGE = 10;
    private static final String SORT_BUTTON_ID_PREFIX = "sort_rank_";
    public static final String PAGINATION_BUTTON_ID_PREFIX = "page_rank_";

    private static final List<RankingCriterion> PRIMARY_CRITERIA = Arrays.asList(
            RankingCriterion.WIN_RATE, RankingCriterion.KDA, RankingCriterion.GAMES
    );
    private static final List<RankingCriterion> SECONDARY_CRITERIA = Arrays.asList(
            RankingCriterion.GPM, RankingCriterion.DPM, RankingCriterion.KP
    );


    /**
     * '/rank-check' 슬래시 커맨드 진입점: 처음부터 상세 순위표를 전송합니다.
     */
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        // 초기 설정: KDA 기준으로 정렬 및 1페이지 시작
        RankingCriterion currentCriterion = RankingCriterion.KDA; // KDA가 좀 더 일반적인 시작 기준이므로 변경
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
        MessageEmbed detailedEmbed = createDetailedRankingEmbed(discordServerId, serverName, allRankedList, currentPageList, currentCriterion, currentPage, totalPages);

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

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 상세 화면 Embed 생성 메서드 (파라미터 변경됨)
    // --------------------------------------------------------------------------------
    public MessageEmbed createDetailedRankingEmbed(Long discordServerId, String serverName, List<UserRankDto> allRankedList, List<UserRankDto> currentPageList, RankingCriterion criterion, int currentPage, int totalPages) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");
        embedBuilder.setColor(new Color(255, 165, 0));
        embedBuilder.setDescription("기준: **" + criterion.getDisplayName() + "** 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상\n"
                + "🔎 **총 " + allRankedList.size() + "명**의 랭커 중 **" + currentPage + "/" + totalPages + "페이지** 표시 중");


        StringBuilder rankingDetailsField = new StringBuilder();

        rankingDetailsField.append("` 순위 | KDA | GPM | DPM | 승률 | K P | 게임 수`\n");
        rankingDetailsField.append("-------------------------------------------------\n");

        int startRank = (currentPage - 1) * ITEMS_PER_PAGE + 1; // 현재 페이지의 시작 순위

        for (int i = 0; i < currentPageList.size(); i++) {
            UserRankDto dto = currentPageList.get(i);

            String rankSymbol = String.valueOf(startRank + i);
            String performanceEmoji = (dto.getKda() >= 5.0 && dto.getWinRate() * 100 >= 60.0) ? "🔥" : "";
            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            String rankFormat = "`%-5s|%5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%|%4d` %s %s\n";

            rankingDetailsField.append(
                    String.format(
                            rankFormat,
                            rankSymbol,
                            dto.getKda(),
                            dto.getGpm(),
                            dto.getDpm(),
                            dto.getWinRate() * 100,
                            dto.getKillParticipation() * 100,
                            dto.getTotalGames(),
                            performanceEmoji,
                            userMention
                    )
            );
        }

        embedBuilder.addField("📊 전체 순위표 (상세 지표)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 페이지 목록을 자르는 메서드 (static public으로 변경)
    // --------------------------------------------------------------------------------
    public static <T> List<T> getPage(List<T> list, int page, int itemsPerPage) {
        int fromIndex = (page - 1) * itemsPerPage;
        if (fromIndex >= list.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + itemsPerPage, list.size());
        return list.subList(fromIndex, toIndex);
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 전체 페이지 수를 계산하는 메서드 (static public으로 변경)
    // --------------------------------------------------------------------------------
    public static int getTotalPages(int totalItems, int itemsPerPage) {
        if (totalItems <= 0) return 0;
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 정렬 버튼 행을 생성하는 메서드 (리스너에서 재사용을 위해 public으로 수정)
    // --------------------------------------------------------------------------------
    public ActionRow createSortButtonsRow1(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, PRIMARY_CRITERIA);
    }

    public ActionRow createSortButtonsRow2(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, SECONDARY_CRITERIA);
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper: 페이지네이션 버튼 행을 생성하는 메서드 (리스너에서 재사용을 위해 public으로 수정)
    // --------------------------------------------------------------------------------
    public ActionRow createPaginationButtonsRowPublic(Long serverId, RankingCriterion activeCriterion, int currentPage, int totalPages) {
        return createPaginationButtonsRow(serverId, activeCriterion, currentPage, totalPages);
    }
}