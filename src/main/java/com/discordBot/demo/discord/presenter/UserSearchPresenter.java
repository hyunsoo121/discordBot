package com.discordBot.demo.discord.presenter;

import com.discordBot.demo.domain.dto.ChampionSearchDto;
import com.discordBot.demo.domain.dto.UserSearchDto;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.awt.Color; // Color import 추가
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserSearchPresenter {

    private static final int CHAMPIONS_PER_PAGE = 5;
    private static final DecimalFormat KDA_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat GPM_DPM_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##%");
    private static final int EMBED_COLOR = 0x6E00CC; // 보라색 계열 (Discord 테마에 잘 어울림)

    public MessageEmbed createUserStatsEmbed(UserSearchDto statsDto) {
        return buildStatsEmbed(statsDto, 0);
    }

    /**
     * 특정 페이지의 Embed를 생성합니다.
     */
    public MessageEmbed buildStatsEmbed(UserSearchDto statsDto, int pageIndex) {
        EmbedBuilder builder = new EmbedBuilder();

        // ⭐ 1. 타이틀 설정: Riot ID와 왕관 아이콘
        String title = String.format("👑 %s#%s님의 내전 종합 지표", statsDto.getSummonerName(), statsDto.getLolTagLine());
        builder.setTitle(title);

        // ⭐ 2. 색상 설정: 통일된 보라색
        builder.setColor(EMBED_COLOR);

        // 3. 롤 계정 목록 (최대 3개 표시)
        String accountList = statsDto.getLinkedLolAccounts().stream()
                .limit(3)
                .map(name -> String.format("`%s`", name))
                .collect(Collectors.joining(" "));

        if (statsDto.getLinkedLolAccounts().size() > 3) {
            accountList += String.format(" 외 %d개", statsDto.getLinkedLolAccounts().size() - 3);
        }

        if (accountList.isEmpty()) {
            accountList = "연결된 롤 계정이 없습니다.";
        }
        builder.addField("🔗 연결 계정", accountList, false); // ⭐ 필드 이름 변경

        // 4. 내전 종합 기록 (판수, 승률, KDA, KP, DPM, GPM)
        String overallStats = String.format(
                "**⚔️ 판수:** %d\n" +
                        "**🎯 승률:** %s\n" +
                        "**💀 KDA:** %s\n" +
                        "**🤝 KP:** %s\n" +
                        "**💥 DPM:** %s\n" +
                        "**💰 GPM:** %s",
                statsDto.getTotalGames(),
                PERCENT_FORMAT.format(statsDto.getWinRate()),
                KDA_FORMAT.format(statsDto.getKda()),
                PERCENT_FORMAT.format(statsDto.getKillParticipation()),
                GPM_DPM_FORMAT.format(statsDto.getDpm()),
                GPM_DPM_FORMAT.format(statsDto.getGpm())
        );
        builder.addField("📈 종합 내전 기록", overallStats, false); // ⭐ 필드 이름 변경

        // 5. 플레이한 챔피언 목록 (페이지네이션)
        List<ChampionSearchDto> allChampions = statsDto.getChampionStatsList();
        int totalPages = (int) Math.ceil((double) allChampions.size() / CHAMPIONS_PER_PAGE);
        int startIndex = pageIndex * CHAMPIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + CHAMPIONS_PER_PAGE, allChampions.size());

        List<ChampionSearchDto> championsOnPage = allChampions.subList(startIndex, endIndex);

        String championList;
        if (championsOnPage.isEmpty()) {
            championList = "기록된 챔피언 통계가 없습니다.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (ChampionSearchDto champ : championsOnPage) {

                String championName = champ.getChampionName();
                String winRate = PERCENT_FORMAT.format(champ.getWinRate());
                String kp = PERCENT_FORMAT.format(champ.getKillParticipation());

                // ⭐ 챔피언 목록 포맷: 이모지를 사용하여 지표를 보기 좋게 분리
                sb.append(String.format(
                        "**⚔️ %s** (`%d판`, 승률 %s)\n" +
                                "   `KDA` %s | `KP` %s | `GPM` %s | `DPM` %s\n",
                        championName,
                        champ.getTotalGames(),
                        winRate,
                        KDA_FORMAT.format(champ.getKda()),
                        kp,
                        GPM_DPM_FORMAT.format(champ.getGpm()),
                        GPM_DPM_FORMAT.format(champ.getDpm())
                ));
            }
            championList = sb.toString();
        }
        builder.addField(String.format("📜 챔피언별 상세 통계 (페이지 %d/%d)", pageIndex + 1, totalPages), championList, false);

        // ⭐ 6. 꼬리말 (Footer) 추가
        builder.setFooter("내전 지표 서비스 | 데이터는 해당 서버의 경기 기록을 기반으로 합니다.");

        return builder.build();
    }

    /**
     * 페이지네이션 버튼을 포함한 ActionRow를 생성합니다.
     */
    public ActionRow createPaginationActionRow(int pageIndex, int totalPages, Long discordUserId) {

        String baseId = "userstats_" + discordUserId + "_";

        Button prev = Button.primary(baseId + "prev_" + (pageIndex - 1), "⬅️ 이전")
                .withDisabled(pageIndex == 0);

        Button next = Button.primary(baseId + "next_" + (pageIndex + 1), "다음 ➡️")
                .withDisabled(pageIndex >= totalPages - 1 || totalPages <= 1);

        return ActionRow.of(prev, next);
    }
}