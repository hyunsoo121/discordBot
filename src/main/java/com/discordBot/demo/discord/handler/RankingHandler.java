package com.discordBot.demo.discord.handler;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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

    private static final String SHOW_BUTTON_ID = "show_rank_details";

    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

        if (rankedList.isEmpty()) {
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
    // Helper 1: 초기 요약 화면 (승률 프로그레스 바) 생성 메서드
    // --------------------------------------------------------------------------------
    public MessageEmbed createSummaryRankingEmbed(Long discordServerId, String serverName, List<UserRankDto> rankedList) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 랭킹 요약");
        embedBuilder.setColor(new Color(58, 204, 87));
        embedBuilder.setDescription("기준: 승률 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        StringBuilder rankingDetailsField = new StringBuilder();

        rankingDetailsField.append("`순위| KDA | 승률`\n");
        rankingDetailsField.append("-----------------------------\n");

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String rankSymbol = String.valueOf(i + 1);
            double winRate = dto.getWinRate() * 100;
            String progressBar = buildProgressBar(winRate);

            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            String rankFormat = "`%-4s|%5.2f|%s %4.0f%%` %s\n";

            rankingDetailsField.append(
                    String.format(
                            rankFormat,
                            rankSymbol, // ⭐ rankSymbol은 이제 1, 2, 3... 입니다.
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
        embedBuilder.setColor(new Color(255, 165, 0));
        embedBuilder.setDescription("기준: 승률 우선 정렬. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        StringBuilder rankingDetailsField = new StringBuilder();

        rankingDetailsField.append("`순위| KDA | GPM | DPM | 승률| KP  `\n");
        rankingDetailsField.append("--------------------------------------\n");

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String rankSymbol = String.valueOf(i + 1);
            String performanceEmoji = (dto.getKda() >= 5.0 && dto.getWinRate() * 100 >= 60.0) ? "🔥" : "";
            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            String rankFormat = "`%-4s|%5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%` %s %s\n";;


            rankingDetailsField.append(
                    String.format(
                            rankFormat,
                            rankSymbol,
                            dto.getKda(),
                            dto.getGpm(),
                            dto.getDpm(),
                            dto.getWinRate() * 100,
                            dto.getKillParticipation() * 100,
                            performanceEmoji,
                            userMention
                    )
            );
        }

        embedBuilder.addField("📊 전체 순위표 (상세 지표)", rankingDetailsField.toString(), false);
        return embedBuilder.build();
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