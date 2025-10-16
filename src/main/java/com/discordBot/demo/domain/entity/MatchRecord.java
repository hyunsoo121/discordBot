package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "MATCH_RECORD", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"server_id", "game_duration_seconds", "blue_total_gold", "red_total_gold"})
})
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 경기가 진행된 서버
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private GuildServer guildServer;

    // 경기 기록 시간 (등록 시점)
    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate = LocalDateTime.now();

    // 승리팀 (예: RED, BLUE)
    @Column(name = "winner_team", nullable = false, length = 10)
    private String winnerTeam;

    // ⭐ 추가/수정: 팀 1 (상단 팀)의 총 골드 합산
    @Column(name = "blue_total_gold", nullable = false)
    private int blueTotalGold;

    // ⭐ 추가/수정: 팀 2 (하단 팀)의 총 골드 합산
    @Column(name = "red_total_gold", nullable = false)
    private int redTotalGold;

    // ⭐ 경기 지속 시간 (초 단위)
    @Column(name = "game_duration_seconds", nullable = false)
    private int gameDurationSeconds;

    @OneToMany(mappedBy = "matchRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerStats> playerStats = new ArrayList<>();

    public void addPlayerStats(PlayerStats stats) {
        this.playerStats.add(stats);
        stats.setMatchRecord(this);
    }
}