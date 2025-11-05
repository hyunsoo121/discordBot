package com.discordBot.demo.discord.presenter;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class MatchImagePresenter {

    private String getDisplayTeamLabel(String dbTeamSide) {
        if (dbTeamSide.equals("BLUE")) {
            return "1팀";
        } else if (dbTeamSide.equals("RED")) {
            return "2팀";
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

            sb.append("`").append(displayTeamLabel).append("` | ");
            sb.append("**").append(stats.getLolGameName()).append("**").append(" (").append(stats.getChampionName()).append("#").append(stats.getLolTagLine()).append(") | ");
            sb.append("KDA: ").append(stats.getKills()).append("/").append(stats.getDeaths()).append("/").append(stats.getAssists()).append("\n");
        });

        return sb.toString();
    }

    public ActionRow createConfirmationButtons(String initiatorId) {

        Button confirmButton = Button.success(MatchImageHandler.BUTTON_ID_CONFIRM + ":" + initiatorId, "✅ 최종 등록");
        Button cancelButton = Button.danger(MatchImageHandler.BUTTON_ID_CANCEL + ":" + initiatorId, "❌ 취소 / 수정");

        return ActionRow.of(confirmButton, cancelButton);
    }

    public String createInitialAnalysisMessage() {
        return "🔍 이미지를 분석 중입니다. 잠시 기다려 주세요... (AI 처리)";
    }
}