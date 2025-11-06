package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.entity.ChampionStats;
import lombok.Builder;
import lombok.Getter;

/**
 * 특정 유저의 특정 챔피언에 대한 통계 지표 DTO.
 */
@Getter
@Builder
public class ChampionSearchDto {

    private String championName;
    private int totalGames;

    // 계산된 통계 지표
    private double kda;
    private double winRate;
    private double killParticipation; // KP
    private double gpm;               // 분당 골드
    private double dpm;               // 분당 피해량

    /**
     * ChampionStats 엔티티로부터 계산된 지표를 포함한 DTO를 생성합니다.
     */
    public static ChampionSearchDto from(ChampionStats stats) {

        int k = stats.getTotalKills();
        int d = stats.getTotalDeaths();
        int a = stats.getTotalAssists();
        int games = stats.getTotalGames();
        int teamKills = stats.getTotalTeamKillsAccumulated();
        long totalGold = stats.getTotalGoldAccumulated();
        long totalDamage = stats.getTotalDamageAccumulated();
        long duration = stats.getTotalDurationSeconds();

        // 1. KDA 계산: (K + A) / Max(1, D)
        double calculatedKda = (double) (k + a) / Math.max(d, 1);

        // 2. 승률 계산
        double calculatedWinRate = (games > 0) ? (double) stats.getTotalWins() / games : 0.0;

        // 3. KP 계산: (Kills + Assists) / Max(1, TeamTotalKills)
        double calculatedKP = (double) (k + a) / Math.max(teamKills, 1);

        // 4. GPM/DPM 계산: (Total / TotalDuration) * 60
        double totalMinutes = (double) duration / 60.0;
        double calculatedGPM = (totalMinutes > 0) ? (double) totalGold / totalMinutes : 0.0;
        double calculatedDPM = (totalMinutes > 0) ? (double) totalDamage / totalMinutes : 0.0;

        return ChampionSearchDto.builder()
                .championName(stats.getChampion().getName())
                .totalGames(games)
                .kda(calculatedKda)
                .winRate(calculatedWinRate)
                .killParticipation(calculatedKP)
                .gpm(calculatedGPM)
                .dpm(calculatedDPM)
                .build();
    }
}