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
    @Column(name = "total_games")
    private int totalGames;

    @Column(name = "total_wins")
    private int totalWins;

    @Column(name = "total_kills")
    private int totalKills;

    @Column(name = "total_deaths")
    private int totalDeaths;

    @Column(name = "total_assists")
    private int totalAssists;



    @Column(name = "total_gold_accumulated")
    private long totalGoldAccumulated;

    @Column(name = "total_damage_accumulated")
    private long totalDamageAccumulated;

    @Column(name = "total_duration_seconds")
    private long totalDurationSeconds; // 누적 게임 시간 (초 단위)


    /**
     * 통계를 업데이트하는 헬퍼 메서드 (Service에서 사용)
     */
    public void addStats(int kills, int deaths, int assists, boolean isWin, long gold, long damage, long durationSeconds) {
        this.totalGames += 1;
        if (isWin) {
            this.totalWins += 1;
        }
        this.totalKills += kills;
        this.totalDeaths += deaths;
        this.totalAssists += assists;
        this.totalGoldAccumulated += gold;
        this.totalDamageAccumulated += damage;
        this.totalDurationSeconds += durationSeconds;
    }
}