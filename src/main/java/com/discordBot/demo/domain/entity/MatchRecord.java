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
@Table(name = "MATCH_RECORD")
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 경기가 진행된 서버
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private GuildServer guildServer;

    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate = LocalDateTime.now(); // 경기 기록 시간

    // 승리팀 (예: RED, BLUE)
    @Column(name = "winner_team", nullable = false, length = 10)
    private String winnerTeam;

    @OneToMany(mappedBy = "matchRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerStats> playerStats = new ArrayList<>();

    public void addPlayerStats(PlayerStats stats) {
        this.playerStats.add(stats);
        stats.setMatchRecord(this);
    }
}