package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "USER_SERVER_STATS")
public class UserServerStats {

    @EmbeddedId
    private UserServerStatsId id;


    @MapsId("user")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("guildServer")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_server_id")
    private GuildServer guildServer;

    @Column(name = "total_kills", nullable = false)
    private int totalKills = 0;

    @Column(name = "total_deaths", nullable = false)
    private int totalDeaths = 0;

    @Column(name = "total_assists", nullable = false)
    private int totalAssists = 0;

    @Column(name = "total_games", nullable = false)
    private int totalGames = 0;

    @Column(name = "total_wins", nullable = false)
    private int totalWins = 0;

    @Column(name = "total_duration_seconds", nullable = false)
    private long totalDurationSeconds = 0;

    @Column(name = "total_gold_accumulated", nullable = false)
    private long totalGoldAccumulated = 0;

    @Column(name = "total_damage_accumulated", nullable = false)
    private long totalDamageAccumulated = 0;

    @Column(name = "total_team_kills_accumulated", nullable = false)
    private int totalTeamKillsAccumulated = 0;
    public void addStats(int kills, int deaths, int assists, boolean isWin,
                         int gold, int damage, int teamTotalKills, long gameDurationSeconds) {
        this.totalKills += kills;
        this.totalDeaths += deaths;
        this.totalAssists += assists;
        this.totalGames += 1;
        if (isWin) {
            this.totalWins += 1;
        }
        this.totalGoldAccumulated += gold;
        this.totalDamageAccumulated += damage;
        this.totalTeamKillsAccumulated += teamTotalKills;
        this.totalDurationSeconds += gameDurationSeconds;
    }
}