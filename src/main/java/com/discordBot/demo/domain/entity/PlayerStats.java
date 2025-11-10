package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "PLAYER_STATS")
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchRecord matchRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lol_id")
    private LolAccount lolNickname; // 해당 경기에 사용된 롤 닉네임

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id", nullable = false)
    private Champion champion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private Line line;

    @Column(name = "team", nullable = false, length = 10)
    private String team; // 속한 팀 (RED/BLUE)

    @Column(name = "is_win", nullable = false)
    private Boolean isWin; // 승패 여부

    @Column(name = "kills", nullable = false)
    private Integer kills;

    @Column(name = "deaths", nullable = false)
    private Integer deaths;

    @Column(name = "assists", nullable = false)
    private Integer assists;

    // ⭐ GPM, DPM 계산을 위한 필드 추가 (PlayerStatsDto와 일치)

    @Column(name = "total_gold", nullable = false) // DTO의 totalGold
    private long totalGold;

    @Column(name = "total_damage", nullable = false) // DTO의 totalDamage
    private long totalDamage;

    @Column(name = "duration_seconds", nullable = false) // DTO의 durationSeconds
    private long durationSeconds;
}