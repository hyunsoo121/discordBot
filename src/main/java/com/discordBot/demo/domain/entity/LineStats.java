package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "LINE_STATS")
public class LineStats {

    @EmbeddedId
    private LineStatsId id;

    @MapsId("user")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("guildServer")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private GuildServer guildServer;

    @MapsId("lineId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private Line line; // 라인 정보

    // ------------------------------------
    // 기본 통계 필드
    // ------------------------------------
    @Column(name = "total_games", nullable = false)
    private int totalGames = 0; // ⭐ 초기값 추가

    @Column(name = "total_wins", nullable = false)
    private int totalWins = 0; // ⭐ 초기값 추가

    @Column(name = "total_kills", nullable = false)
    private int totalKills = 0; // ⭐ 초기값 추가

    @Column(name = "total_deaths", nullable = false)
    private int totalDeaths = 0; // ⭐ 초기값 추가

    @Column(name = "total_assists", nullable = false)
    private int totalAssists = 0; // ⭐ 초기값 추가


    @Column(name = "total_gold_accumulated", nullable = false)
    private long totalGoldAccumulated = 0; // ⭐ 초기값 추가

    @Column(name = "total_damage_accumulated", nullable = false)
    private long totalDamageAccumulated = 0; // ⭐ 초기값 추가

    // ⭐ 추가: 킬 관여율 (KP) 계산을 위한 누적 팀 킬 필드
    @Column(name = "total_team_kills_accumulated", nullable = false)
    private int totalTeamKillsAccumulated = 0; // ⭐ 추가 및 초기값 설정

    @Column(name = "total_duration_seconds", nullable = false)
    private long totalDurationSeconds = 0; // ⭐ 초기값 추가


    /**
     * 통계를 업데이트하는 헬퍼 메서드 (Service에서 사용)
     * ChampionStats의 addStats 서명과 일치하도록 teamTotalKills 파라미터 추가
     */
    public void addStats(int kills, int deaths, int assists, boolean isWin,
                         long gold, long damage, int teamTotalKills, long gameDurationSeconds) { // ⭐ 파라미터 수정

        this.totalGames += 1;
        if (isWin) {
            this.totalWins += 1;
        }
        this.totalKills += kills;
        this.totalDeaths += deaths;
        this.totalAssists += assists;
        this.totalGoldAccumulated += gold;
        this.totalDamageAccumulated += damage;
        this.totalTeamKillsAccumulated += teamTotalKills; // ⭐ 필드 업데이트
        this.totalDurationSeconds += gameDurationSeconds;
    }
}