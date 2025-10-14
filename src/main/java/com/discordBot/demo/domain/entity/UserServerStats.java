package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "USER_SERVER_STATS") // 테이블 이름도 명확하게 변경
public class UserServerStats {

    // ⭐ 복합 키 설정 (User + GuildServer)
    @EmbeddedId
    private UserServerStatsId id;

    // User 엔티티 매핑
    @MapsId("user")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // GuildServer 엔티티 매핑
    @MapsId("guildServer")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_server_id")
    private GuildServer guildServer;

    // -------------------------------------------------------------------------
    // ⭐ 누적 통계 필드 (KDA 및 승률 계산용)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------

    /**
     * 누적 통계를 업데이트하는 편의 메서드
     * @param kills 해당 경기 킬 수
     * @param deaths 해당 경기 데스 수
     * @param assists 해당 경기 어시스트 수
     * @param isWin 해당 경기 승리 여부
     */
    public void addStats(int kills, int deaths, int assists, boolean isWin) {
        this.totalKills += kills;
        this.totalDeaths += deaths;
        this.totalAssists += assists;
        this.totalGames += 1;
        if (isWin) {
            this.totalWins += 1;
        }
    }
}