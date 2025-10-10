package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "LOL_ACCOUNT", uniqueConstraints = {
        // ⭐ 서버별 롤 계정 고유성: 이 세 가지 조합이 DB 전체에서 유일해야 함
        @UniqueConstraint(columnNames = {"game_name", "tag_line", "guild_server_id"}),
})
public class LolAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lolId;

    @Column(name = "game_name", nullable = false, length = 100)
    private String gameName;

    @Column(name = "tag_line", length = 10)
    private String tagLine;

    // Puuid는 Riot API에서 제공하는 글로벌 고유 ID
    @Column(name = "puuid", length = 78, nullable = false)
    private String puuid;

    // 롤 계정의 소유자 (한 유저가 여러 롤 계정을 가질 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // ⭐ 롤 계정이 등록된 서버 (한 서버에 여러 롤 계정이 등록됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_server_id", nullable = false) // 서버별 등록을 위해 필수
    private GuildServer guildServer;

    public String getFullAccountName() {
        return gameName + "#" + (tagLine != null ? tagLine : "");
    }
}
