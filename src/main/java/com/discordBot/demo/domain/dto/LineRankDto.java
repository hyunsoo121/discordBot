package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.entity.LineStats;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LineRankDto {

    private Long discordUserId;
    private Long lineId;
    private String lineName;
    private int totalGames;

    private double killParticipation; // KP
    private double gpm;               // 분당 골드
    private double dpm;               // 분당 피해량

    private double kda;
    private double winRate;

    public static LineRankDto from(LineStats stats) {
        Long userId = stats.getUser() != null ? stats.getUser().getDiscordUserId() : 0L;

        int k = stats.getTotalKills();
        int d = stats.getTotalDeaths();
        int a = stats.getTotalAssists();
        int games = stats.getTotalGames();
        int teamKills = stats.getTotalTeamKillsAccumulated();
        long totalGold = stats.getTotalGoldAccumulated();
        long totalDamage = stats.getTotalDamageAccumulated();
        long duration = stats.getTotalDurationSeconds();

        double calculatedKda = (double) (k + a) / Math.max(d, 1);
        double calculatedWinRate = (games > 0) ? (double) stats.getTotalWins() / games : 0.0;
        double calculatedKP = (double) (k + a) / Math.max(teamKills, 1);

        double totalMinutes = (double) duration / 60.0;
        double calculatedGPM = (totalMinutes > 0) ? (double) totalGold / totalMinutes : 0.0;
        double calculatedDPM = (totalMinutes > 0) ? (double) totalDamage / totalMinutes : 0.0;

        String lineName = stats.getLine() != null ? stats.getLine().getName() : "UNKNOWN";
        Long lineId = stats.getLine() != null ? stats.getLine().getLineId() : 0L;

        return LineRankDto.builder()
                .discordUserId(userId)
                .lineId(lineId)
                .lineName(lineName)
                .totalGames(games)
                .kda(calculatedKda)
                .winRate(calculatedWinRate)
                .killParticipation(calculatedKP)
                .gpm(calculatedGPM)
                .dpm(calculatedDPM)
                .build();
    }
}
