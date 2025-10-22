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
import java.util.Arrays; // Arrays 임포트 추가
import java.util.stream.Collectors; // Collectors 임포트 추가

@Component
@RequiredArgsConstructor
public class RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    private static final String SORT_BUTTON_ID_PREFIX = "sort_rank_";

    // ⭐ Helper: 모든 정렬 기준 Enum을 목록으로 가져옵니다.
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

        RankingCriterion currentCriterion = RankingCriterion.WIN_RATE;

        List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

        if (rankedList.isEmpty()) {
            event.getHook().sendMessage("❌ 현재 서버에는 랭킹 데이터가 없습니다.").queue();
            return;
        }

        // 1. Embed 생성: 상세 Embed를 호출
        MessageEmbed detailedEmbed = createDetailedRankingEmbed(discordServerId, serverName, rankedList, currentCriterion);

        // ⭐⭐ 수정: 6가지 정렬 버튼을 두 ActionRow로 분리하여 생성 ⭐⭐
        ActionRow sortRow1 = createSortButtonsRow(discordServerId, currentCriterion, PRIMARY_CRITERIA);
        ActionRow sortRow2 = createSortButtonsRow(discordServerId, currentCriterion, SECONDARY_CRITERIA);


        // 3. 메시지 전송 (두 개의 정렬 버튼 ActionRow 포함)
        event.getHook().sendMessageEmbeds(detailedEmbed)
                .setComponents(
                        sortRow1,
                        sortRow2
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
    // ⭐ Helper: 상세 화면 (전체 5가지 지표) 생성 메서드 (로직 유지)
    // --------------------------------------------------------------------------------
    public MessageEmbed createDetailedRankingEmbed(Long discordServerId, String serverName, List<UserRankDto> rankedList, RankingCriterion criterion) {
        // ... (Embed 생성 및 순위표 포맷팅 로직 유지) ...
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");
        embedBuilder.setColor(new Color(255, 165, 0));
        embedBuilder.setDescription("기준: **" + criterion.getDisplayName() + "** 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        StringBuilder rankingDetailsField = new StringBuilder();

        rankingDetailsField.append("`순위| KDA | GPM | DPM | 승률| KP | 게임수`\n");
        rankingDetailsField.append("-------------------------------------------\n");

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String rankSymbol = String.valueOf(i + 1);
            String performanceEmoji = (dto.getKda() >= 5.0 && dto.getWinRate() * 100 >= 60.0) ? "🔥" : "";
            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            String rankFormat = "`%-4s|%5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%|%4d` %s %s\n";

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
}