package com.discordBot.demo.discord.presenter;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingPresenter {

    private static final int MIN_GAMES_THRESHOLD = 1;
    private static final int ITEMS_PER_PAGE = 10;
    private static final List<RankingCriterion> PRIMARY_CRITERIA = Arrays.asList(
            RankingCriterion.WIN_RATE, RankingCriterion.KDA, RankingCriterion.GAMES
    );
    private static final List<RankingCriterion> SECONDARY_CRITERIA = Arrays.asList(
            RankingCriterion.GPM, RankingCriterion.DPM, RankingCriterion.KP
    );


    public MessageEmbed createDetailedRankingEmbed(String serverName, List<UserRankDto> allRankedList, List<UserRankDto> currentPageList, RankingCriterion criterion, int currentPage, int totalPages) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");

        // --- 색상 및 스타일 로직 ---
        Color embedColor;
        switch (criterion) {
            case WIN_RATE: embedColor = new Color(0, 255, 0); break;
            case KDA: embedColor = new Color(255, 69, 0); break;
            case GAMES: embedColor = new Color(173, 216, 230); break;
            case GPM: embedColor = new Color(255, 215, 0); break;
            case DPM: embedColor = new Color(255, 0, 0); break;
            case KP: embedColor = new Color(138, 43, 226); break;
            default: embedColor = new Color(255, 165, 0);
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
                            rankSymbol, dto.getKda(), dto.getGpm(), dto.getDpm(),
                            dto.getKillParticipation() * 100, dto.getWinRate() * 100, dto.getTotalGames(),
                            performanceEmoji, userMention
                    )
            );
        }

        embedBuilder.addField("📊 전체 순위표 (상세 지표)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
    }

    // --- 버튼 생성 로직 ---
    public ActionRow createSortButtonsRow1(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, PRIMARY_CRITERIA);
    }

    public ActionRow createSortButtonsRow2(Long serverId, RankingCriterion activeCriterion) {
        return createSortButtonsRow(serverId, activeCriterion, SECONDARY_CRITERIA);
    }

    public ActionRow createPaginationButtonsRow(Long serverId, RankingCriterion activeCriterion, int currentPage, int totalPages) {
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
}