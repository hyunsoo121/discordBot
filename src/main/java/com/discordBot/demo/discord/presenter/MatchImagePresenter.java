package com.discordBot.demo.discord.presenter;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class MatchImagePresenter {

    private String getDisplayTeamLabel(String dbTeamSide) {
        if (dbTeamSide.equals("BLUE")) {
            return "1팀 (🟦)";
        } else if (dbTeamSide.equals("RED")) {
            return "2팀 (🟥)";
        }
        return dbTeamSide;
    }

    public String createConfirmationMessageContent(MatchRegistrationDto dto) {

        StringBuilder sb = new StringBuilder();
        sb.append("✅ **AI 분석 완료!** 아래 기록이 정확합니까? (업로더만 확인할 수 있습니다)\n\n");

        String winnerTeamLabel = getDisplayTeamLabel(dto.getWinnerTeam());
        sb.append("🏆 승리팀: **").append(winnerTeamLabel).append("**\n\n");

        dto.getPlayerStatsList().forEach(stats -> {
            String displayTeamLabel = getDisplayTeamLabel(stats.getTeam());

            String laneCode = stats.getLaneName();
            String laneDisplay = (StringUtils.hasText(laneCode) && !laneCode.equalsIgnoreCase("UNKNOWN"))
                    ? laneCode
                    : "라인 미확인";

            sb.append("`").append(displayTeamLabel).append("` | ");

            sb.append("**").append(stats.getLolGameName()).append("#").append(stats.getLolTagLine()).append("**");
            sb.append(" (").append(stats.getChampionName()).append(" / **").append(laneDisplay).append("**) | ");

            sb.append("KDA: ").append(stats.getKills()).append("/").append(stats.getDeaths()).append("/").append(stats.getAssists()).append("\n");
        });

        return sb.toString();
    }

    // ⭐ tempMatchId를 포함하여 버튼 ID를 생성하는 메서드 (MatchImageHandlerImpl에서 사용)
    public List<ActionRow> createConfirmationButtonsWithId(String initiatorId, Long tempMatchId, List<PlayerStatsDto> playerStatsList) {
        return createConfirmationComponents(initiatorId, tempMatchId);
    }

    // ⭐ createConfirmationComponents 메서드 (이전에 논의된 최종 구조)
    public List<ActionRow> createConfirmationComponents(String initiatorId, Long tempMatchId) {

        // 1. 등록/취소 버튼 행
        // ID 포맷: ACTION:INITIATOR_ID:TEMP_MATCH_ID
        Button confirmButton = Button.success(MatchImageHandler.BUTTON_ID_CONFIRM + ":" + initiatorId + ":" + tempMatchId, "✅ 최종 등록");
        Button cancelButton = Button.danger(MatchImageHandler.BUTTON_ID_CANCEL + ":" + initiatorId + ":" + tempMatchId, "❌ 취소");

        ActionRow actionRow1 = ActionRow.of(confirmButton, cancelButton);

        // 2. 수정 버튼 행 (BLUE 팀)
        ActionRow actionRowBlue = createEditButtonRow("BLUE", initiatorId, tempMatchId);

        // 3. 수정 버튼 행 (RED 팀)
        ActionRow actionRowRed = createEditButtonRow("RED", initiatorId, tempMatchId);

        List<ActionRow> actionRows = new ArrayList<>();
        actionRows.add(actionRow1);
        actionRows.add(actionRowBlue);
        actionRows.add(actionRowRed);

        return actionRows;
    }

    // 수정 버튼 행 생성 헬퍼
    private ActionRow createEditButtonRow(String team, String initiatorId, Long tempMatchId) {
        String teamEmoji = team.equals("BLUE") ? "🟦" : "🟥";

        // 버튼 ID 포맷: BUTTON_ID_EDIT:initiatorId:tempMatchId:Team:Category

        Button champButton = Button.secondary(
                MatchImageHandler.BUTTON_ID_EDIT + ":" + initiatorId + ":" + tempMatchId + ":" + team + ":CHAMP",
                teamEmoji + " 챔피언"
        );
        Button laneButton = Button.secondary(
                MatchImageHandler.BUTTON_ID_EDIT + ":" + initiatorId + ":" + tempMatchId + ":" + team + ":LANE",
                teamEmoji + " 라인"
        );
        Button accountButton = Button.secondary(
                MatchImageHandler.BUTTON_ID_EDIT + ":" + initiatorId + ":" + tempMatchId + ":" + team + ":ACCOUNT",
                teamEmoji + " 계정명"
        );

        return ActionRow.of(champButton, laneButton, accountButton);
    }

    public String createInitialAnalysisMessage() {
        return "🔍 이미지를 분석 중입니다. 잠시 기다려 주세요... (AI 처리)";
    }

    public String createEditSuccessMessage(PlayerStatsDto stats) {
        return String.format("✅ **%s (%s)** 기록이 수정되었습니다. 최종 등록을 진행해주세요.",
                stats.getLolGameName() + "#" + stats.getLolTagLine(),
                getDisplayTeamLabel(stats.getTeam()));
    }
}