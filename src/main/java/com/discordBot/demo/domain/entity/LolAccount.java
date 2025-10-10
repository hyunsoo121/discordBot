package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "LOL_ACCOUNT")
public class LolAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // 라이엇 ID의 게임 이름 (예: Hide On Bush)
    @Column(name = "game_name", nullable = false, length = 50)
    private String gameName;

    // 라이엇 ID의 태그 (예: KR1)
    @Column(name = "tag_line", nullable = true, length = 10)
    private String tagLine;

    @Column(name = "puuid", unique = true, nullable = true, length = 78)
    private String puuid;

    public String getFullAccountName() {
        return this.gameName + "#" + this.tagLine;
    }
}