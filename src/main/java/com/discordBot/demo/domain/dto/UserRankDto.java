package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.entity.UserServerStats;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRankDto {

    private Long discordUserId;
    private int totalGames;

    private double killParticipation; // KP
    private double gpm;               // 분당 골드
    private double dpm;               // 분당 피해량

    private double kda;
    private double winRate;

    public static UserRankDto from(UserServerStats stats) {

        Long userId = stats.getUser() != null ? stats.getUser().getDiscordUserId() : 0L;

        int k = stats.getTotalKills();
        int d = stats.getTotalDeaths();
        int a = stats.getTotalAssists();
        int games = stats.getTotalGames();
        int teamKills = stats.getTotalTeamKillsAccumulated();
        long totalGold = stats.getTotalGoldAccumulated();
        long totalDamage = stats.getTotalDamageAccumulated();
        long duration = stats.getTotalDurationSeconds();

        // 1. KDA 계산
        double calculatedKda = (double) (k + a) / Math.max(d, 1);

        // 2. 승률 계산
        double calculatedWinRate = (games > 0) ? (double) stats.getTotalWins() / games : 0.0;

        // 3. KP 계산: (Kills + Assists) / Max(1, TeamTotalKills)
        double calculatedKP = (double) (k + a) / Math.max(teamKills, 1);

        // 4. GPM/DPM 계산: (Total / TotalDuration) * 60 (총 시간이 0이면 0 처리)
        double totalMinutes = (double) duration / 60.0;
        double calculatedGPM = (totalMinutes > 0) ? (double) totalGold / totalMinutes : 0.0;
        double calculatedDPM = (totalMinutes > 0) ? (double) totalDamage / totalMinutes : 0.0;

        return UserRankDto.builder()
                .discordUserId(userId)
                .totalGames(games)
                .kda(calculatedKda)
                .winRate(calculatedWinRate)
                .killParticipation(calculatedKP)
                .gpm(calculatedGPM)
                .dpm(calculatedDPM)
                .build();
    }
}