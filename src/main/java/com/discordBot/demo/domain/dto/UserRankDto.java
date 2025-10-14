package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.entity.UserServerStats;
import lombok.Builder;
import lombok.Getter;

/**
 * 랭킹 목록 또는 개인 통계를 보여주기 위한 DTO.
 * 엔티티의 raw 데이터(Kills, Deaths, Wins)를 가공하여 승률, KDA를 포함합니다.
 */
@Getter
@Builder
public class UserRankDto {

    // ⭐ 디스코드 유저 ID만 사용 (이름은 봇 출력 시 API로 조회 가정)
    private Long discordUserId;

    // 누적 통계
    private int totalGames;
    private int totalWins;
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;

    // 계산된 지표
    private double winRate;         // 승률 (0.00 ~ 1.00)
    private double kda;             // KDA (K+A) / D

    /**
     * UserServerStats 엔티티를 기반으로 UserRankDto를 생성하는 팩토리 메서드.
     */
    public static UserRankDto from(UserServerStats stats) {

        // ⭐ User 엔티티에서 ID만 가져옵니다. (User가 null일 경우 방지)
        Long userId = stats.getUser() != null ? stats.getUser().getDiscordUserId() : 0L;

        int k = stats.getTotalKills();
        int d = stats.getTotalDeaths();
        int a = stats.getTotalAssists();
        int games = stats.getTotalGames();
        int wins = stats.getTotalWins();

        // KDA 계산: (K + A) / D. 데스가 0일 경우 1로 간주
        double calculatedKda = (double) (k + a) / Math.max(d, 1);

        // 승률 계산
        double calculatedWinRate = (games > 0) ? (double) wins / games : 0.0;

        return UserRankDto.builder()
                // ⭐ 수정된 부분: ID만 사용
                .discordUserId(userId)
                .totalGames(games)
                .totalWins(wins)
                .totalKills(k)
                .totalDeaths(d)
                .totalAssists(a)
                .winRate(calculatedWinRate)
                .kda(calculatedKda)
                .build();
    }
}