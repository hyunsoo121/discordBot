package com.discordBot.demo.discord.presenter;

import com.discordBot.demo.domain.dto.ChampionSearchDto;
import com.discordBot.demo.domain.dto.UserSearchDto;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserSearchPresenter {

    private static final int CHAMPIONS_PER_PAGE = 5;
    private static final DecimalFormat KDA_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat GPM_DPM_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

    public MessageEmbed createUserStatsEmbed(UserSearchDto statsDto) {
        return buildStatsEmbed(statsDto, 0); // 첫 페이지 (index 0) 생성
    }

    /**
     * 특정 페이지의 Embed를 생성합니다.
     */
    public MessageEmbed buildStatsEmbed(UserSearchDto statsDto, int pageIndex) {
        EmbedBuilder builder = new EmbedBuilder();

        // 1. Title 설정
        String title = String.format("%s#%s님의 내전 기록", statsDto.getSummonerName(), statsDto.getLolTagLine());
        builder.setTitle(title);
        builder.setColor(0x00FF00); // 녹색

        // 2. 롤 계정 목록 (최대 3개 표시)
        String accountList = statsDto.getLinkedLolAccounts().stream()
                .limit(3)
                .map(name -> String.format("`%s`", name))
                .collect(Collectors.joining(" ")); // 한 줄에 공백으로 구분

        if (statsDto.getLinkedLolAccounts().size() > 3) {
            accountList += String.format(" 외 %d개", statsDto.getLinkedLolAccounts().size() - 3);
        }

        if (accountList.isEmpty()) {
            accountList = "연결된 롤 계정이 없습니다.";
        }
        builder.addField("롤 계정 (최대 3개)", accountList, false);

        // 3. 내전 종합 기록 (판수, 승률, KDA, KP, DPM, GPM)
        String overallStats = String.format(
                "**판수:** %d\n**승률:** %s\n**KDA:** %s\n**KP:** %s\n**DPM:** %s\n**GPM:** %s",
                statsDto.getTotalGames(),
                PERCENT_FORMAT.format(statsDto.getWinRate()),
                KDA_FORMAT.format(statsDto.getKda()),
                PERCENT_FORMAT.format(statsDto.getKillParticipation()),
                GPM_DPM_FORMAT.format(statsDto.getDpm()),
                GPM_DPM_FORMAT.format(statsDto.getGpm())
        );
        builder.addField("내전 종합 기록", overallStats, false);

        // 4. 플레이한 챔피언 목록 (페이지네이션)
        List<ChampionSearchDto> allChampions = statsDto.getChampionStatsList();
        int totalPages = (int) Math.ceil((double) allChampions.size() / CHAMPIONS_PER_PAGE);
        int startIndex = pageIndex * CHAMPIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + CHAMPIONS_PER_PAGE, allChampions.size());

        List<ChampionSearchDto> championsOnPage = allChampions.subList(startIndex, endIndex);

        String championList;
        if (championsOnPage.isEmpty()) {
            championList = "기록된 챔피언 통계가 없습니다.";
        } else {
            // 헤더: 챔피언 이름 | 판수 | 승률 | KDA | KP | DPM
            StringBuilder sb = new StringBuilder("`챔피언 | 판수 | 승률 | KDA | KP | DPM`\n");
            for (ChampionSearchDto champ : championsOnPage) {
                sb.append(String.format("`%-4s | %-4d | %-5s | %-4s | %-3s | %-4s`\n",
                        champ.getChampionName().length() > 4 ? champ.getChampionName().substring(0, 4) : champ.getChampionName(), // 이름 4글자 제한
                        champ.getTotalGames(),
                        PERCENT_FORMAT.format(champ.getWinRate()).replace("%", ""),
                        KDA_FORMAT.format(champ.getKda()),
                        PERCENT_FORMAT.format(champ.getKillParticipation()).replace("%", ""),
                        GPM_DPM_FORMAT.format(champ.getDpm())
                ));
            }
            championList = sb.toString();
        }
        builder.addField(String.format("챔피언 목록 (페이지 %d/%d)", pageIndex + 1, totalPages), championList, false);

        return builder.build();
    }

    /**
     * 페이지네이션 버튼을 포함한 ActionRow를 생성합니다.
     */
    public ActionRow createPaginationActionRow(int pageIndex, int totalPages, Long discordUserId) {
        String baseId = "userstats_" + discordUserId + "_";

        Button prev = Button.primary(baseId + "prev_" + (pageIndex), "⬅️ 이전") // 이전 페이지 인덱스를 포함
                .withDisabled(pageIndex == 0);
        Button next = Button.primary(baseId + "next_" + (pageIndex), "다음 ➡️") // 현재 페이지 인덱스를 포함
                .withDisabled(pageIndex >= totalPages - 1 || totalPages <= 1);

        return ActionRow.of(prev, next);
    }
}