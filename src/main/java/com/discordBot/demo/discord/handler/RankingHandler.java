package com.discordBot.demo.discord.handler;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed; // MessageEmbed 임포트
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    // 버튼 ID 상수 정의
    private static final String SHOW_BUTTON_ID = "show_rank_details";
    private static final String HIDE_BUTTON_ID = "hide_rank_details";


    /**
     * '/rank-check' 슬래시 커맨드 진입점: 초기 요약 화면(승률 프로그레스 바)을 전송합니다.
     */
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue(); // 본인에게만 보이게 설정 (Listener에서 이미 처리되나, 안전을 위해 남겨둠)

        // ... (유효성 검사 및 데이터 조회 로직 생략) ...
        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

        if (rankedList.isEmpty()) {
            // ... (데이터 없음 처리 로직 생략) ...
            event.getHook().sendMessage("❌ 현재 서버에는 랭킹 데이터가 없습니다.").queue();
            return;
        }

        // 1. 초기 요약 Embed 생성
        MessageEmbed summaryEmbed = createSummaryRankingEmbed(discordServerId, serverName, rankedList);

        // 2. 버튼 생성 (Show Details 버튼)
        Button showDetailsButton = Button.primary(SHOW_BUTTON_ID + "_" + discordServerId, "🔍 상세 지표 보기");

        // 3. 메시지 전송
        event.getHook().sendMessageEmbeds(summaryEmbed)
                .setComponents(ActionRow.of(showDetailsButton))
                .queue();
    }

    // --------------------------------------------------------------------------------
    // ⭐ Helper 1: 초기 요약 화면 (승률 프로그레스 바) 생성 메서드
    // --------------------------------------------------------------------------------
    public MessageEmbed createSummaryRankingEmbed(Long discordServerId, String serverName, List<UserRankDto> rankedList) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 랭킹 요약");
        embedBuilder.setColor(new Color(58, 204, 87));
        embedBuilder.setDescription("기준: KDA 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        StringBuilder rankingDetailsField = new StringBuilder();

        // 헤더: 순위 | KDA | 승률 (프로그레스바)
        rankingDetailsField.append("`순위| KDA | 승률`\n");
        rankingDetailsField.append("-----------------------------\n");

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String rankSymbol = getRankIcon(i);
            double winRate = dto.getWinRate() * 100;
            String progressBar = buildProgressBar(winRate); // 프로그레스 바 생성

            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            rankingDetailsField.append(
                    String.format(
                            // 폭 포맷: 순위(4)| KDA(5)| 승률바 + %
                            "`%-4s|%5.2f|%s %4.0f%%` %s\n",
                            rankSymbol,
                            dto.getKda(),
                            progressBar,
                            winRate,
                            userMention
                    )
            );
        }

        embedBuilder.addField("✅ 전체 순위표 (요약)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
    }


    // --------------------------------------------------------------------------------
    // ⭐ Helper 2: 상세 화면 (전체 5가지 지표) 생성 메서드
    // --------------------------------------------------------------------------------
    public MessageEmbed createDetailedRankingEmbed(Long discordServerId, String serverName, List<UserRankDto> rankedList) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표 (상세)");
        embedBuilder.setColor(new Color(255, 165, 0)); // 색상 변경으로 구분
        embedBuilder.setDescription("기준: KDA 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        StringBuilder rankingDetailsField = new StringBuilder();

        // 헤더: 모든 지표 포함 (승률은 단순 %로)
        rankingDetailsField.append("`순위| KDA | GPM | DPM | 승률| KP  `\n");
        rankingDetailsField.append("--------------------------------------\n");

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String rankSymbol = getRankIcon(i);
            String performanceEmoji = (dto.getKda() >= 5.0 && dto.getWinRate() * 100 >= 60.0) ? "🔥" : "";
            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            rankingDetailsField.append(
                    String.format(
                            // 폭 포맷: 순위(4)| KDA(5)| GPM(5)| DPM(5)| 승률(4)| KP(4)
                            "`%-4s|%5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%` %s %s\n",
                            rankSymbol,
                            dto.getKda(),
                            dto.getGpm(),
                            dto.getDpm(),
                            dto.getWinRate() * 100, // ⭐ 승률 단순 % 출력
                            dto.getKillParticipation() * 100,
                            performanceEmoji,
                            userMention
                    )
            );
        }

        embedBuilder.addField("📊 전체 순위표 (상세 지표)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
    }

    // --------------------------------------------------------------------------------
    // Helper Methods (재사용)
    // --------------------------------------------------------------------------------
    private String getRankIcon(int index) {
        return switch (index) {
            case 0 -> "1️⃣"; case 1 -> "2️⃣"; case 2 -> "3️⃣"; case 3 -> "4️⃣";
            case 4 -> "5️⃣"; case 5 -> "6️⃣"; case 6 -> "7️⃣"; case 7 -> "8️⃣";
            case 8 -> "9️⃣"; case 9 -> "🔟";
            default -> String.valueOf(index + 1);
        };
    }

    private String buildProgressBar(double percentage) {
        int barLength = 10;
        int filled = (int) Math.round(percentage / 100.0 * barLength);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        return bar.toString();
    }
}