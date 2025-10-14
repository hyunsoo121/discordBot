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
     * @param event 디스코드 슬래시 커맨드 이벤트
     */
    public void handleRankingCommand(SlashCommandInteractionEvent event) {

        // 🚨 중요: SlashCommandListener에서 이미 event.deferReply(true/false)가 호출되었다고 가정합니다.
        //         따라서 여기서는 event.reply() 대신 event.getHook()을 사용해야 합니다.

        // 이 명령은 서버(길드) 내에서만 실행 가능합니다.
        if (!event.isFromGuild()) {
            // 이 명령은 deferReply 이전에 실행되어야 하므로, 이 부분은 예외 처리 필요
            // 여기서는 getHook()을 사용할 수 없으므로, Listener에서 걸러내야 함.
            return;
        }

        Long discordServerId = event.getGuild().getIdLong();
        String serverName = event.getGuild().getName();

        // 1. RankingService를 통해 랭킹 데이터 조회 및 계산
        List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

        // 2. 응답 메시지 생성
        if (rankedList.isEmpty()) {
            String message = String.format("❌ 현재 '%s' 서버에는 랭킹 데이터가 없습니다.\n(최소 %d게임 이상 기록해야 순위에 포함됩니다.)",
                    serverName, MIN_GAMES_THRESHOLD);

            // ⭐ 수정: getHook()을 사용하여 메시지 전송. ephemeral 설정은 Listener의 deferReply(true)를 따릅니다.
            event.getHook().sendMessage(message).queue();
            return;
        }

        // 3. 임베드 메시지 구성 (순위표)
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🏆 " + serverName + " ⚔️ 내전 랭킹 순위표");
        embedBuilder.setColor(new Color(58, 204, 87));
        embedBuilder.setDescription("기준: KDA (킬/데스/어시스트) 순, 최소 " + MIN_GAMES_THRESHOLD + "게임 이상");

        // 필드 내용 구성 (로직 유지)
        StringBuilder rankField = new StringBuilder();
        StringBuilder kdaField = new StringBuilder();
        StringBuilder winRateField = new StringBuilder();

        int limit = Math.min(rankedList.size(), 10);

        for (int i = 0; i < limit; i++) {
            UserRankDto dto = rankedList.get(i);
            // ... (순위표 구성 로직 유지)
            String rankSymbol = switch (i) {
                case 0 -> "🥇"; case 1 -> "🥈"; case 2 -> "🥉"; default -> (i + 1) + ".";
            };
            rankField.append(String.format("%s <@%d>\n", rankSymbol, dto.getDiscordUserId()));
            kdaField.append(String.format("%.2f\n", dto.getKda()));
            double winRatePercent = dto.getWinRate() * 100;
            winRateField.append(String.format("%.1f%% (%dG)\n", winRatePercent, dto.getTotalGames()));
        }

        // 필드 추가
        embedBuilder.addField("순위 (유저)", rankField.toString(), true);
        embedBuilder.addField("KDA", kdaField.toString(), true);
        embedBuilder.addField("승률 (총 게임)", winRateField.toString(), true);

        // 4. 메시지 전송
        // ⭐ 수정: getHook()을 사용하여 Embed 전송. (Listener의 deferReply 설정을 따름)
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}