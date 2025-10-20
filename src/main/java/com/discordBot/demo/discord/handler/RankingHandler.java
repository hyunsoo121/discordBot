package com.discordBot.demo.discord.handler;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingHandler {

    private final RankingService rankingService;
    private static final int MIN_GAMES_THRESHOLD = 1;

    /**
     * '/rank-check' 슬래시 커맨드를 처리하고 랭킹 순위표를 디스코드에 출력합니다.
     */
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        try {
            event.deferReply(true).queue(); // 본인에게만 보이게 설정
        } catch (IllegalStateException e) {}

        if (!event.isFromGuild()) {
            event.getHook().sendMessage("❌ 이 명령어는 디스코드 서버 내에서만 사용 가능합니다.").setEphemeral(true).queue();
            return;
        }

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        // 1. 랭킹 데이터 조회
        List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

        if (rankedList.isEmpty()) {
            String message = String.format("❌ 현재 '%s' 서버에는 랭킹 데이터가 없습니다.\n(최소 %d경기 이상 기록해야 순위에 포함됩니다.)",
                    serverName, MIN_GAMES_THRESHOLD);
            event.getHook().sendMessage(message).queue();
            return;
        }

        // 2. 임베드 메시지 구성
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 통합 랭킹 순위표");
        embedBuilder.setColor(new Color(58, 204, 87));
        embedBuilder.setDescription("기준: KDA 우선. 최소 " + MIN_GAMES_THRESHOLD + "경기 이상");

        // ⭐ 통합 필드 내용을 담을 StringBuilder
        StringBuilder rankingDetailsField = new StringBuilder();

        // ⭐⭐ 수정: 헤더 순서 및 폭 조정 (유저 멘션은 표 밖에)
        rankingDetailsField.append("`순위| KDA | GPM | DPM | 승률| KP  `\n");
        rankingDetailsField.append("--------------------------------------\n"); // 너비에 맞춰 조정

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);

            String userMention = String.format("<@%d>", dto.getDiscordUserId());

            rankingDetailsField.append(
                    String.format(
                            // 폭 포맷: 순위(4)| KDA(5)| GPM(5)| DPM(5)| 승률(4)| KP(4)
                            "`%-4s|%5.2f|%-5.0f|%-5.0f|%-4.0f%%|%-4.0f%%` %s\n",
                            i + 1, // 순위
                            dto.getKda(),
                            dto.getGpm(),
                            dto.getDpm(),
                            dto.getWinRate() * 100,
                            dto.getKillParticipation() * 100,
                            userMention // 사용자 멘션은 표 밖에 출력
                    )
            );
        }

        // 3. 필드 추가
        // ⭐ 수정: 필드 제목을 '전체 순위표'로 통일
        embedBuilder.addField("전체 순위표 (지표 / 유저)", rankingDetailsField.toString(), false);

        // 4. 메시지 전송
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}